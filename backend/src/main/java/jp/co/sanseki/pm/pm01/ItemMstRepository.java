package jp.co.sanseki.pm.pm01;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jp.co.sanseki.pm.common.paging.KeysetSource;
import jp.co.sanseki.pm.common.util.FixedChar;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** ITEM_MST アクセス。一覧・照会は DEL_FLG='0' のみを対象とする（論理削除行は除外）。 */
@Repository
public class ItemMstRepository implements KeysetSource<ItemMst> {

    private static final String COLUMNS = "ITEM_CD, ITEM_NAME, ITEM_KBN, STOCK_UNIT, DEL_FLG, UPD_TMS, CRT_TMS";

    private final JdbcClient jdbc;

    public ItemMstRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ItemMst> findActive(String itemCd) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM ITEM_MST WHERE ITEM_CD = :cd AND DEL_FLG = '0'")
                .param("cd", itemCd)
                .query(ItemMstRepository::map)
                .optional();
    }

    /** 論理削除行も含めた存在確認（主キー重複チェック）。 */
    public boolean exists(String itemCd) {
        return jdbc.sql("SELECT COUNT(*) FROM ITEM_MST WHERE ITEM_CD = :cd")
                .param("cd", itemCd)
                .query(Integer.class)
                .single() > 0;
    }

    public void insert(ItemMst item) {
        jdbc.sql("""
                INSERT INTO ITEM_MST (ITEM_CD, ITEM_NAME, ITEM_KBN, STOCK_UNIT, DEL_FLG, UPD_TMS, CRT_TMS)
                VALUES (:cd, :name, :kbn, :unit, :del, :upd, :crt)""")
                .param("cd", item.itemCd())
                .param("name", item.itemName())
                .param("kbn", item.itemKbn())
                .param("unit", item.stockUnit())
                .param("del", item.delFlg())
                .param("upd", item.updTms())
                .param("crt", item.crtTms())
                .update();
    }

    /** 楽観的排他制御付き更新。照会時の UPD_TMS と一致した場合のみ更新し、更新件数を返す。 */
    public int updateIfUnchanged(String itemCd, String itemName, String itemKbn, String stockUnit,
                                 LocalDateTime heldTms, LocalDateTime newTms) {
        return jdbc.sql("""
                UPDATE ITEM_MST
                   SET ITEM_NAME = :name, ITEM_KBN = :kbn, STOCK_UNIT = :unit, UPD_TMS = :newTms
                 WHERE ITEM_CD = :cd AND DEL_FLG = '0' AND UPD_TMS = :heldTms""")
                .param("name", itemName)
                .param("kbn", itemKbn)
                .param("unit", stockUnit)
                .param("newTms", newTms)
                .param("cd", itemCd)
                .param("heldTms", heldTms)
                .update();
    }

    /** 論理削除（DEL_FLG='1' の更新のみ。物理削除はしない）。楽観的排他制御付き。 */
    public int logicalDeleteIfUnchanged(String itemCd, LocalDateTime heldTms, LocalDateTime newTms) {
        return jdbc.sql("""
                UPDATE ITEM_MST
                   SET DEL_FLG = '1', UPD_TMS = :newTms
                 WHERE ITEM_CD = :cd AND DEL_FLG = '0' AND UPD_TMS = :heldTms""")
                .param("newTms", newTms)
                .param("cd", itemCd)
                .param("heldTms", heldTms)
                .update();
    }

    @Override
    public List<ItemMst> first(int limit) {
        return list("SELECT " + COLUMNS + " FROM ITEM_MST WHERE DEL_FLG = '0' ORDER BY ITEM_CD LIMIT :limit", null, limit);
    }

    @Override
    public List<ItemMst> fromKey(String key, int limit) {
        return list("SELECT " + COLUMNS + " FROM ITEM_MST WHERE DEL_FLG = '0' AND ITEM_CD >= :key ORDER BY ITEM_CD LIMIT :limit", key, limit);
    }

    @Override
    public List<ItemMst> afterKey(String key, int limit) {
        return list("SELECT " + COLUMNS + " FROM ITEM_MST WHERE DEL_FLG = '0' AND ITEM_CD > :key ORDER BY ITEM_CD LIMIT :limit", key, limit);
    }

    @Override
    public List<ItemMst> beforeKey(String key, int limit) {
        return list("SELECT " + COLUMNS + " FROM ITEM_MST WHERE DEL_FLG = '0' AND ITEM_CD < :key ORDER BY ITEM_CD DESC LIMIT :limit", key, limit);
    }

    @Override
    public String keyOf(ItemMst row) {
        return row.itemCd();
    }

    private List<ItemMst> list(String sql, String key, int limit) {
        var spec = jdbc.sql(sql).param("limit", limit);
        if (key != null) {
            spec = spec.param("key", key);
        }
        return spec.query(ItemMstRepository::map).list();
    }

    private static ItemMst map(ResultSet rs, int rowNum) throws SQLException {
        return new ItemMst(
                FixedChar.rtrim(rs.getString("ITEM_CD")),
                FixedChar.rtrim(rs.getString("ITEM_NAME")),
                FixedChar.rtrim(rs.getString("ITEM_KBN")),
                FixedChar.rtrim(rs.getString("STOCK_UNIT")),
                FixedChar.rtrim(rs.getString("DEL_FLG")),
                rs.getTimestamp("UPD_TMS").toLocalDateTime(),
                rs.getTimestamp("CRT_TMS").toLocalDateTime());
    }
}
