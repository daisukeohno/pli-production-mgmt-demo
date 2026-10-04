package jp.sanseki.pm.pm01;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.sanseki.pm.common.commarea.Commarea;
import jp.sanseki.pm.common.msg.PmBusinessException;
import jp.sanseki.pm.common.msg.PmMessage;
import jp.sanseki.pm.common.util.Db2Timestamp;
import jp.sanseki.pm.common.util.FixedChar;
import jp.sanseki.pm.common.util.Pmutl01;
import jp.sanseki.pm.common.web.Aid;

/**
 * PM01 品目マスタ保守のメイン処理。
 * 1 回の呼び出しが疑似会話の 1 タスク（RECEIVE MAP → 業務処理 → SEND MAP → RETURN COMMAREA）に相当する。
 */
@Service
public class Pm01Service {

    public static final String SCRN_ID = "PM01";
    public static final int PAGE_SIZE = 10;

    static final String FUNC_INQUIRY = "1";
    static final String FUNC_REGISTER = "2";
    static final String FUNC_UPDATE = "3";
    static final String FUNC_DELETE = "4";

    private final ItemMstRepository repo;
    private final Pmutl01 pmutl01;

    public Pm01Service(ItemMstRepository repo, Pmutl01 pmutl01) {
        this.repo = repo;
        this.pmutl01 = pmutl01;
    }

    /** 処理結果（画面出力＋業務メッセージ）。 */
    public record Result(Pm01Response response, PmMessage msg) {
    }

    /** 初回起動（CA-FIRST-TIME='Y'）。空画面と一覧 1 頁目を表示する。 */
    @Transactional(readOnly = true)
    public Result start(Commarea ca) {
        initCommarea(ca);
        return send(ca, emptyFields(""), null, null, false);
    }

    @Transactional
    public Result handle(Commarea ca, Pm01Request in) {
        if (!SCRN_ID.equals(ca.header.scrnId) || Commarea.FIRST_TIME_YES.equals(ca.header.firstTime)) {
            initCommarea(ca);
        }
        ca.msg.clear();
        Aid aid = in.aid() == null ? Aid.ENTER : in.aid();
        Pm01Response.Fields input = inputFields(in);

        return switch (aid) {
            case CLEAR -> start(ca);
            case PF3 -> send(ca, emptyFields(""), null, null, true);
            case PF7 -> {
                prevPage(ca);
                yield send(ca, input, null, null, false);
            }
            case PF8 -> {
                nextPage(ca);
                yield send(ca, input, null, null, false);
            }
            case ENTER -> enter(ca, input);
            // PM01 で未定義の PF キーは無視して再表示
            default -> send(ca, input, null, null, false);
        };
    }

    // ------------------------------------------------------------------
    // ENTER: F-FUNC による機能分岐
    // ------------------------------------------------------------------
    private Result enter(Commarea ca, Pm01Response.Fields in) {
        try {
            String func = in.func();
            if (func.isEmpty() || !List.of(FUNC_INQUIRY, FUNC_REGISTER, FUNC_UPDATE, FUNC_DELETE).contains(func)) {
                throw error(PmMessage.M004, Pm01Field.F_FUNC);
            }
            ca.header.funcCd = func;
            // F-ITEMNM / F-KBN / F-UNIT は機能 2,3 のみ入力可（1,4 では保護項目として受け取らない）
            Pm01Response.Fields f = (FUNC_REGISTER.equals(func) || FUNC_UPDATE.equals(func))
                    ? in : emptyFields(func, in.itemCd());
            return switch (func) {
                case FUNC_INQUIRY -> inquiry(ca, f);
                case FUNC_REGISTER -> register(ca, f);
                case FUNC_UPDATE -> update(ca, f);
                default -> delete(ca, f);
            };
        } catch (PmBusinessException e) {
            return sendError(ca, in, e);
        }
    }

    /** 機能 1: 照会。UPD_TMS を CA01-UPD-TMS へ退避する。 */
    private Result inquiry(Commarea ca, Pm01Response.Fields f) {
        requireItemCd(f.itemCd());
        ItemMst item = findOrM002(ca, f.itemCd());
        select(ca, item);
        return send(ca, toFields(f.func(), item), null, null, false);
    }

    /** 機能 2: 登録。チェックデジット検証（不正は M001）。 */
    private Result register(Commarea ca, Pm01Response.Fields f) {
        validateItemCdWithChkDgt(f.itemCd());
        validateAttributes(f);
        if (repo.exists(f.itemCd())) {
            throw error(PmMessage.M003, Pm01Field.F_ITEMCD);
        }
        LocalDateTime now = pmutl01.currentTimestamp();
        try {
            repo.insert(f.itemCd(), f.itemName(), f.itemKbn(), f.stockUnit(), now);
        } catch (DuplicateKeyException e) {
            // 存在確認後に他端末が同一コードを登録した場合（SQLCODE -803 相当）
            throw error(PmMessage.M003, Pm01Field.F_ITEMCD);
        }
        ca.pm01.itemCd = f.itemCd();
        ca.pm01.updTms = Db2Timestamp.format(now);
        return send(ca, f, PmMessage.M005, null, false);
    }

    /** 機能 3: 更新。チェックデジット検証＋楽観的排他制御（不一致は M017）。 */
    private Result update(Commarea ca, Pm01Response.Fields f) {
        validateItemCdWithChkDgt(f.itemCd());
        if (!isSelected(ca, f.itemCd())) {
            // 未照会の品目：まず現在値を表示して UPD_TMS を退避し、再度 ENTER で更新する
            ItemMst item = findOrM002(ca, f.itemCd());
            select(ca, item);
            return send(ca, toFields(f.func(), item), null, Pm01Field.F_ITEMNM, false);
        }
        validateAttributes(f);
        ItemMst current = lockAndCompare(ca, f.itemCd());
        LocalDateTime newTms = nextTimestamp(current.updTms());
        repo.update(f.itemCd(), f.itemName(), f.itemKbn(), f.stockUnit(), newTms);
        ca.pm01.updTms = Db2Timestamp.format(newTms);
        return send(ca, f, PmMessage.M006, null, false);
    }

    /** 機能 4: 削除。DEL_FLG='1' の論理削除＋楽観的排他制御。 */
    private Result delete(Commarea ca, Pm01Response.Fields f) {
        requireItemCd(f.itemCd());
        if (!isSelected(ca, f.itemCd())) {
            ItemMst item = findOrM002(ca, f.itemCd());
            select(ca, item);
            return send(ca, toFields(f.func(), item), null, Pm01Field.F_FUNC, false);
        }
        ItemMst current = lockAndCompare(ca, f.itemCd());
        repo.logicalDelete(f.itemCd(), nextTimestamp(current.updTms()));
        ca.pm01.clearSelection();
        return send(ca, emptyFields(f.func(), f.itemCd()), PmMessage.M007, null, false);
    }

    /** 更新・削除直前に再読込し、CA01-UPD-TMS と比較する。 */
    private ItemMst lockAndCompare(Commarea ca, String itemCd) {
        Optional<ItemMst> current = repo.findActiveForUpdate(itemCd);
        if (current.isEmpty()) {
            ca.pm01.clearSelection();
            throw error(PmMessage.M002, Pm01Field.F_ITEMCD);
        }
        if (!FixedChar.equalsChar(Db2Timestamp.format(current.get().updTms()), ca.pm01.updTms)) {
            throw error(PmMessage.M017, Pm01Field.F_FUNC);
        }
        return current.get();
    }

    // ------------------------------------------------------------------
    // 入力チェック
    // ------------------------------------------------------------------
    private static void requireItemCd(String itemCd) {
        if (itemCd.isEmpty()) {
            throw error(PmMessage.M004, Pm01Field.F_ITEMCD);
        }
        if (!FixedChar.fits(itemCd, ItemMst.ITEM_CD_LEN)) {
            throw error(PmMessage.M004, Pm01Field.F_ITEMCD);
        }
    }

    /** PMUTL01 VALID-CHK-DGT（P-FUNC='V'）を呼び出し、P-VALID-FLG='N' なら M001。 */
    private static void validateItemCdWithChkDgt(String itemCd) {
        if (itemCd.isEmpty()) {
            throw error(PmMessage.M004, Pm01Field.F_ITEMCD);
        }
        if (!Pmutl01.validChkDgt(itemCd)) {
            throw error(PmMessage.M001, Pm01Field.F_ITEMCD);
        }
    }

    private static void validateAttributes(Pm01Response.Fields f) {
        if (f.itemName().isEmpty() || !FixedChar.fits(f.itemName(), ItemMst.ITEM_NAME_LEN)) {
            throw error(PmMessage.M004, Pm01Field.F_ITEMNM);
        }
        if (!ItemMst.isValidKbn(f.itemKbn())) {
            throw error(PmMessage.M004, Pm01Field.F_KBN);
        }
        if (f.stockUnit().isEmpty() || !FixedChar.fits(f.stockUnit(), ItemMst.STOCK_UNIT_LEN)) {
            throw error(PmMessage.M004, Pm01Field.F_UNIT);
        }
    }

    // ------------------------------------------------------------------
    // 一覧（10 行/頁、CA01-TOP-ITEM-CD 起点のキーセット）
    // ------------------------------------------------------------------
    private void nextPage(Commarea ca) {
        List<ItemMst> rows = repo.listFrom(pageStartKey(ca), PAGE_SIZE + 1);
        if (rows.size() > PAGE_SIZE) {
            ca.pm01.topItemCd = rows.get(PAGE_SIZE).itemCd();
            ca.pm01.pageNo++;
        }
    }

    private void prevPage(Commarea ca) {
        if (ca.pm01.pageNo <= 1) {
            ca.pm01.pageNo = 1;
            return;
        }
        List<String> keys = repo.keysBefore(ca.pm01.topItemCd, PAGE_SIZE);
        if (keys.size() < PAGE_SIZE || ca.pm01.pageNo - 1 <= 1) {
            ca.pm01.pageNo = 1;
        } else {
            ca.pm01.pageNo--;
            ca.pm01.topItemCd = keys.get(keys.size() - 1);
        }
    }

    private List<Pm01Response.ListRow> readPage(Commarea ca) {
        List<ItemMst> rows = repo.listFrom(pageStartKey(ca), PAGE_SIZE);
        if (rows.isEmpty() && ca.pm01.pageNo > 1) {
            // 当該頁の品目が全て削除された場合は前頁へ戻す
            prevPage(ca);
            rows = repo.listFrom(pageStartKey(ca), PAGE_SIZE);
        }
        ca.pm01.topItemCd = rows.isEmpty() ? "" : rows.get(0).itemCd();
        List<Pm01Response.ListRow> list = new ArrayList<>();
        for (ItemMst r : rows) {
            list.add(new Pm01Response.ListRow(r.itemCd(), r.itemName(), r.itemKbn()));
        }
        return list;
    }

    /** 1 頁目は常に先頭から、2 頁目以降は CA01-TOP-ITEM-CD から読む。 */
    private static String pageStartKey(Commarea ca) {
        return ca.pm01.pageNo <= 1 ? "" : ca.pm01.topItemCd;
    }

    // ------------------------------------------------------------------
    // 共通
    // ------------------------------------------------------------------
    private static void initCommarea(Commarea ca) {
        ca.header.scrnId = SCRN_ID;
        ca.header.funcCd = "";
        ca.header.firstTime = Commarea.FIRST_TIME_NO;
        ca.pm01.clearSelection();
        ca.pm01.pageNo = 1;
        ca.pm01.topItemCd = "";
        ca.msg.clear();
    }

    private ItemMst findOrM002(Commarea ca, String itemCd) {
        Optional<ItemMst> item = repo.findActive(itemCd);
        if (item.isEmpty()) {
            ca.pm01.clearSelection();
            throw error(PmMessage.M002, Pm01Field.F_ITEMCD);
        }
        return item.get();
    }

    private static void select(Commarea ca, ItemMst item) {
        ca.pm01.itemCd = item.itemCd();
        ca.pm01.updTms = Db2Timestamp.format(item.updTms());
    }

    private static boolean isSelected(Commarea ca, String itemCd) {
        return !ca.pm01.updTms.isEmpty() && FixedChar.equalsChar(ca.pm01.itemCd, itemCd);
    }

    /** CURRENT TIMESTAMP。旧値と同一になった場合は 1 マイクロ秒進めて排他キーの一意性を保つ。 */
    private LocalDateTime nextTimestamp(LocalDateTime old) {
        LocalDateTime now = pmutl01.currentTimestamp();
        return now.isAfter(old) ? now : old.plusNanos(1_000);
    }

    private static PmBusinessException error(PmMessage msg, Pm01Field field) {
        return new PmBusinessException(msg, field.bmsName());
    }

    private Result sendError(Commarea ca, Pm01Response.Fields in, PmBusinessException e) {
        Pm01Field field = null;
        for (Pm01Field f : Pm01Field.values()) {
            if (f.bmsName().equals(e.fieldPos())) {
                field = f;
            }
        }
        return send(ca, in, e.msg(), field, false);
    }

    private Result send(Commarea ca, Pm01Response.Fields fields, PmMessage msg, Pm01Field cursor, boolean ended) {
        ca.msg.msgId = msg == null ? "" : msg.msgId();
        ca.msg.msgFldPos = cursor == null ? 0 : cursor.order();
        List<Pm01Response.ListRow> list = ended ? List.of() : readPage(ca);
        Pm01Response res = new Pm01Response(
                SCRN_ID,
                pmutl01.getSysDate().replace('-', '/'),
                msg == null ? null : msg.msgId(),
                msg == null ? null : msg.msgText(),
                cursor == null ? null : cursor.bmsName(),
                fields,
                list,
                ca.pm01.pageNo,
                ended);
        return new Result(res, msg);
    }

    private static Pm01Response.Fields inputFields(Pm01Request in) {
        return new Pm01Response.Fields(
                FixedChar.rtrim(in.func()),
                FixedChar.rtrim(in.itemCd()),
                FixedChar.rtrim(in.itemName()),
                FixedChar.rtrim(in.itemKbn()),
                FixedChar.rtrim(in.stockUnit()));
    }

    private static Pm01Response.Fields emptyFields(String func) {
        return emptyFields(func, "");
    }

    private static Pm01Response.Fields emptyFields(String func, String itemCd) {
        return new Pm01Response.Fields(func, itemCd, "", "", "");
    }

    private static Pm01Response.Fields toFields(String func, ItemMst item) {
        return new Pm01Response.Fields(func, item.itemCd(), item.itemName(), item.itemKbn(), item.stockUnit());
    }
}
