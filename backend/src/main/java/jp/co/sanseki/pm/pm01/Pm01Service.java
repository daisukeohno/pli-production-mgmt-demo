package jp.co.sanseki.pm.pm01;

import java.time.LocalDateTime;

import jp.co.sanseki.pm.common.domain.ItemKbn;
import jp.co.sanseki.pm.common.error.BusinessException;
import jp.co.sanseki.pm.common.message.MessageCatalog;
import jp.co.sanseki.pm.common.message.MessageIds;
import jp.co.sanseki.pm.common.paging.KeysetPage;
import jp.co.sanseki.pm.common.paging.KeysetPager;
import jp.co.sanseki.pm.common.paging.PageMove;
import jp.co.sanseki.pm.common.util.CheckDigit;
import jp.co.sanseki.pm.common.util.Db2Timestamp;
import jp.co.sanseki.pm.common.util.FixedChar;
import jp.co.sanseki.pm.common.util.SystemClock;
import jp.co.sanseki.pm.pm01.Pm01Dto.DeleteResponse;
import jp.co.sanseki.pm.pm01.Pm01Dto.ItemRequest;
import jp.co.sanseki.pm.pm01.Pm01Dto.ItemResponse;
import jp.co.sanseki.pm.pm01.Pm01Dto.ListResponse;
import jp.co.sanseki.pm.pm01.Pm01Dto.ListRow;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PM01 品目マスタ保守。機能コード 1:照会 2:登録 3:更新 4:削除 と一覧（PF7/PF8）。
 * 入力チェックは BMS 項目順（品目コード→品目名→区分→単位）に行い、最初のエラーで止める。
 */
@Service
public class Pm01Service {

    /** 一覧の表示件数（F-LIST01〜F-LIST10）。運用マニュアルの20件/頁は旧仕様。 */
    public static final int PAGE_SIZE = 10;

    private final ItemMstRepository repository;
    private final MessageCatalog messages;
    private final SystemClock clock;
    private final KeysetPager<ItemMst> pager;

    public Pm01Service(ItemMstRepository repository, MessageCatalog messages, SystemClock clock) {
        this.repository = repository;
        this.messages = messages;
        this.clock = clock;
        this.pager = new KeysetPager<>(repository, PAGE_SIZE);
    }

    /** 一覧。cursor=CA01-TOP-ITEM-CD、pageNo=CA01-PAGE-NO。 */
    @Transactional(readOnly = true)
    public ListResponse list(PageMove move, String cursor, int pageNo) {
        KeysetPage<ListRow> page = pager.page(move, blankToNull(cursor), pageNo).map(ListRow::of);
        return new ListResponse(page.rows(), page.cursor(), page.pageNo(), page.hasPrev(), page.hasNext());
    }

    /** 機能コード1：照会。 */
    @Transactional(readOnly = true)
    public ItemResponse inquire(String itemCd) {
        String cd = FixedChar.rtrim(itemCd);
        if (cd.isEmpty()) {
            throw BusinessException.badRequest(MessageIds.M004, Pm01Field.ITEM_CD);
        }
        return ItemResponse.of(findActive(cd), null, null);
    }

    /** 機能コード2：登録。 */
    @Transactional
    public ItemResponse register(ItemRequest req) {
        String cd = validateItemCd(req.itemCd());
        Attributes attrs = validateAttributes(req);
        if (repository.exists(cd)) {
            throw BusinessException.conflict(MessageIds.M003, Pm01Field.ITEM_CD);
        }
        LocalDateTime now = clock.currentTimestamp();
        ItemMst item = new ItemMst(cd, attrs.name, attrs.kbn, attrs.unit, ItemMst.DEL_FLG_ACTIVE, now, now);
        try {
            repository.insert(item);
        } catch (DuplicateKeyException e) {
            throw BusinessException.conflict(MessageIds.M003, Pm01Field.ITEM_CD);
        }
        return ItemResponse.of(item, MessageIds.M005, messages.text(MessageIds.M005));
    }

    /** 機能コード3：更新。照会時の UPD_TMS（CA01-UPD-TMS）を更新直前に再比較し、不一致は M017。 */
    @Transactional
    public ItemResponse update(String itemCd, ItemRequest req) {
        String cd = validateItemCd(itemCd);
        Attributes attrs = validateAttributes(req);
        ItemMst current = findActive(cd);
        LocalDateTime held = verifyUpdTms(req.updTms(), current);
        int updated = repository.updateIfUnchanged(cd, attrs.name, attrs.kbn, attrs.unit, held, nextTimestamp(held));
        if (updated == 0) {
            throw optimisticLockError();
        }
        return ItemResponse.of(findActive(cd), MessageIds.M006, messages.text(MessageIds.M006));
    }

    /** 機能コード4：削除（論理削除）。照会時の UPD_TMS を再比較し、不一致は M017。 */
    @Transactional
    public DeleteResponse delete(String itemCd, String updTms) {
        String cd = FixedChar.rtrim(itemCd);
        if (cd.isEmpty()) {
            throw BusinessException.badRequest(MessageIds.M004, Pm01Field.ITEM_CD);
        }
        ItemMst current = findActive(cd);
        LocalDateTime held = verifyUpdTms(updTms, current);
        if (repository.logicalDeleteIfUnchanged(cd, held, nextTimestamp(held)) == 0) {
            throw optimisticLockError();
        }
        return new DeleteResponse(cd, MessageIds.M007, messages.text(MessageIds.M007));
    }

    private ItemMst findActive(String cd) {
        return repository.findActive(cd)
                .orElseThrow(() -> BusinessException.notFound(MessageIds.M002, Pm01Field.ITEM_CD));
    }

    /** 品目コード：必須（M004）、数字8桁かつ末尾チェックデジット一致（M001）。 */
    private String validateItemCd(String itemCd) {
        String cd = FixedChar.rtrim(itemCd);
        if (cd.isEmpty()) {
            throw BusinessException.badRequest(MessageIds.M004, Pm01Field.ITEM_CD);
        }
        if (!CheckDigit.isValid(cd)) {
            throw BusinessException.badRequest(MessageIds.M001, Pm01Field.ITEM_CD);
        }
        return cd;
    }

    private Attributes validateAttributes(ItemRequest req) {
        String name = requireFixed(req.itemName(), ItemMst.ITEM_NAME_LEN, Pm01Field.ITEM_NAME);
        String kbn = requireFixed(req.itemKbn(), ItemMst.ITEM_KBN_LEN, Pm01Field.ITEM_KBN);
        if (ItemKbn.of(kbn).isEmpty()) {
            throw BusinessException.badRequest(MessageIds.X001, Pm01Field.ITEM_KBN);
        }
        String unit = requireFixed(req.stockUnit(), ItemMst.STOCK_UNIT_LEN, Pm01Field.STOCK_UNIT);
        return new Attributes(name, kbn, unit);
    }

    private static String requireFixed(String value, int length, Pm01Field field) {
        if (FixedChar.isBlank(value)) {
            throw BusinessException.badRequest(MessageIds.M004, field);
        }
        if (!FixedChar.fits(value, length)) {
            throw BusinessException.badRequest(MessageIds.X002, field);
        }
        return FixedChar.rtrim(value);
    }

    private static LocalDateTime verifyUpdTms(String updTms, ItemMst current) {
        LocalDateTime held = Db2Timestamp.tryParse(updTms).orElseThrow(Pm01Service::optimisticLockError);
        if (!held.equals(current.updTms())) {
            throw optimisticLockError();
        }
        return held;
    }

    /** 新しい UPD_TMS。同一マイクロ秒内の連続更新でも必ず変わるようにする。 */
    private LocalDateTime nextTimestamp(LocalDateTime held) {
        LocalDateTime now = clock.currentTimestamp();
        return now.isAfter(held) ? now : held.plusNanos(1_000);
    }

    private static BusinessException optimisticLockError() {
        return BusinessException.conflict(MessageIds.M017, Pm01Field.ITEM_CD);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private record Attributes(String name, String kbn, String unit) {
    }
}
