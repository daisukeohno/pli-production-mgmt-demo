package jp.co.sanseki.pm.item;

import java.util.Optional;

import jp.co.sanseki.pm.common.Chars;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ItemRepository {

    static final RowMapper<ItemMst> ROW_MAPPER = (rs, i) -> new ItemMst(
            Chars.trimPadding(rs.getString("ITEM_CD")),
            Chars.trimPadding(rs.getString("ITEM_NAME")),
            Chars.trimPadding(rs.getString("ITEM_KBN")),
            Chars.trimPadding(rs.getString("STOCK_UNIT")),
            Chars.trimPadding(rs.getString("DEL_FLG")),
            rs.getTimestamp("UPD_TMS").toLocalDateTime(),
            rs.getTimestamp("CRT_TMS").toLocalDateTime());

    private final JdbcClient jdbc;

    public ItemRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** 論理削除済み（DEL_FLG='1'）も含めて主キーで読む。 */
    public Optional<ItemMst> findByItemCd(String itemCd) {
        return jdbc.sql("""
                SELECT ITEM_CD, ITEM_NAME, ITEM_KBN, STOCK_UNIT, DEL_FLG, UPD_TMS, CRT_TMS
                  FROM SANSEKI.ITEM_MST
                 WHERE ITEM_CD = :itemCd
                """)
                .param("itemCd", itemCd)
                .query(ROW_MAPPER)
                .optional();
    }

    public long countActive() {
        return jdbc.sql("SELECT COUNT(*) FROM SANSEKI.ITEM_MST WHERE DEL_FLG = '0'").query(Long.class).single();
    }
}
