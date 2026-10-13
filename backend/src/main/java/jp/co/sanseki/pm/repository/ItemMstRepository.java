package jp.co.sanseki.pm.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jp.co.sanseki.pm.domain.ItemMst;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemMstRepository extends JpaRepository<ItemMst, String> {

    Optional<ItemMst> findByItemCdAndDelFlg(String itemCd, String delFlg);

    List<ItemMst> findByDelFlgOrderByItemCdAsc(String delFlg, Limit limit);

    List<ItemMst> findByDelFlgAndItemCdGreaterThanEqualOrderByItemCdAsc(String delFlg, String itemCd, Limit limit);

    List<ItemMst> findByDelFlgAndItemCdLessThanOrderByItemCdDesc(String delFlg, String itemCd, Limit limit);

    /** 楽観排他つき更新（WHERE UPD_TMS = 照会時の値）。更新件数 0 は他端末で更新済み。 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ItemMst i
               set i.itemName = :itemName, i.itemKbn = :itemKbn, i.stockUnit = :stockUnit, i.updTms = :newUpdTms
             where i.itemCd = :itemCd and i.delFlg = '0' and i.updTms = :heldUpdTms
            """)
    int updateIfUnchanged(@Param("itemCd") String itemCd,
                          @Param("itemName") String itemName,
                          @Param("itemKbn") String itemKbn,
                          @Param("stockUnit") String stockUnit,
                          @Param("heldUpdTms") LocalDateTime heldUpdTms,
                          @Param("newUpdTms") LocalDateTime newUpdTms);

    /** 論理削除（DEL_FLG='1'）。物理削除はしない。 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ItemMst i
               set i.delFlg = '1', i.updTms = :newUpdTms
             where i.itemCd = :itemCd and i.delFlg = '0' and i.updTms = :heldUpdTms
            """)
    int logicalDeleteIfUnchanged(@Param("itemCd") String itemCd,
                                 @Param("heldUpdTms") LocalDateTime heldUpdTms,
                                 @Param("newUpdTms") LocalDateTime newUpdTms);
}
