package jp.co.sanseki.pm.service;

import java.time.LocalDateTime;

import jp.co.sanseki.pm.common.error.BusinessException;
import jp.co.sanseki.pm.common.lock.OptimisticLock;
import jp.co.sanseki.pm.common.message.MessageIds;
import jp.co.sanseki.pm.common.paging.CursorPage;
import jp.co.sanseki.pm.common.paging.KeysetPager;
import jp.co.sanseki.pm.common.paging.PageMove;
import jp.co.sanseki.pm.domain.ItemMst;
import jp.co.sanseki.pm.dto.ItemCreateRequest;
import jp.co.sanseki.pm.dto.ItemListRow;
import jp.co.sanseki.pm.dto.ItemResponse;
import jp.co.sanseki.pm.dto.ItemUpdateRequest;
import jp.co.sanseki.pm.repository.ItemMstRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PM01 品目マスタ保守。一覧・照会は DEL_FLG='0' のみ対象。 */
@Service
public class ItemService {

    public static final int PAGE_SIZE = 10;
    private static final String ITEM_CD = "itemCd";

    private final ItemMstRepository repository;
    private final OptimisticLock optimisticLock;
    private final KeysetPager<ItemMst> pager;

    public ItemService(ItemMstRepository repository, OptimisticLock optimisticLock) {
        this.repository = repository;
        this.optimisticLock = optimisticLock;
        this.pager = new KeysetPager<>(
                (from, limit) -> from == null
                        ? repository.findByDelFlgOrderByItemCdAsc(ItemMst.DEL_FLG_ACTIVE, Limit.of(limit))
                        : repository.findByDelFlgAndItemCdGreaterThanEqualOrderByItemCdAsc(ItemMst.DEL_FLG_ACTIVE, from, Limit.of(limit)),
                (before, limit) -> repository.findByDelFlgAndItemCdLessThanOrderByItemCdDesc(ItemMst.DEL_FLG_ACTIVE, before, Limit.of(limit)),
                ItemMst::getItemCd);
    }

    /** 一覧（品目コード昇順・10件/頁）。PF7=PREV、PF8=NEXT。 */
    @Transactional(readOnly = true)
    public CursorPage<ItemListRow> list(PageMove move, String topItemCd, int pageNo, int size) {
        return pager.page(move, topItemCd, pageNo, size).map(ItemListRow::from);
    }

    /** 照会（機能コード1）。 */
    @Transactional(readOnly = true)
    public ItemResponse find(String itemCd) {
        return ItemResponse.from(findActive(itemCd));
    }

    /** 登録（機能コード2）。チェックデジットは DTO の @ValidItemCd で検証済み（不正は M001）。 */
    @Transactional
    public ItemResponse create(ItemCreateRequest request) {
        if (repository.existsById(request.itemCd())) {
            throw BusinessException.conflict(MessageIds.M003, ITEM_CD);
        }
        ItemMst item = ItemMst.newItem(request.itemCd(), request.itemName(), request.itemKbn(), request.stockUnit(),
                optimisticLock.now());
        try {
            repository.saveAndFlush(item);
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.conflict(MessageIds.M003, ITEM_CD);
        }
        return ItemResponse.from(item);
    }

    /** 更新（機能コード3）。更新直前に UPD_TMS を再比較し、不一致は M017。 */
    @Transactional
    public ItemResponse update(String itemCd, ItemUpdateRequest request) {
        ItemMst current = findActive(itemCd);
        LocalDateTime held = optimisticLock.verify(request.updTms(), current.getUpdTms());
        int updated = repository.updateIfUnchanged(itemCd, request.itemName(), request.itemKbn(), request.stockUnit(),
                held, optimisticLock.nextTimestamp(held));
        optimisticLock.requireUpdated(updated);
        return ItemResponse.from(findActive(itemCd));
    }

    /** 削除（機能コード4）。論理削除（DEL_FLG='1'）。削除直前に UPD_TMS を再比較し、不一致は M017。 */
    @Transactional
    public void delete(String itemCd, String updTms) {
        ItemMst current = findActive(itemCd);
        LocalDateTime held = optimisticLock.verify(updTms, current.getUpdTms());
        int updated = repository.logicalDeleteIfUnchanged(itemCd, held, optimisticLock.nextTimestamp(held));
        optimisticLock.requireUpdated(updated);
    }

    private ItemMst findActive(String itemCd) {
        return repository.findByItemCdAndDelFlg(itemCd, ItemMst.DEL_FLG_ACTIVE)
                .orElseThrow(() -> BusinessException.notFound(MessageIds.M002, ITEM_CD));
    }
}
