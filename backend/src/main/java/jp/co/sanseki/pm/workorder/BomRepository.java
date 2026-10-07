package jp.co.sanseki.pm.workorder;

import java.util.List;

import jp.co.sanseki.pm.common.Chars;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class BomRepository {

    static final RowMapper<Bom> ROW_MAPPER = (rs, i) -> new Bom(
            Chars.trimPadding(rs.getString("PARENT_ITEM_CD")),
            Chars.trimPadding(rs.getString("CHILD_ITEM_CD")),
            rs.getBigDecimal("QTY_PER"),
            rs.getBigDecimal("YIELD_RATE"),
            rs.getTimestamp("UPD_TMS").toLocalDateTime());

    private final JdbcClient jdbc;

    public BomRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** 親品目の子部品を子品目コード昇順で返す。 */
    public List<Bom> findByParentItemCd(String parentItemCd) {
        return jdbc.sql("""
                SELECT PARENT_ITEM_CD, CHILD_ITEM_CD, QTY_PER, YIELD_RATE, UPD_TMS
                  FROM SANSEKI.BOM
                 WHERE PARENT_ITEM_CD = :parentItemCd
                 ORDER BY CHILD_ITEM_CD
                """)
                .param("parentItemCd", parentItemCd)
                .query(ROW_MAPPER)
                .list();
    }
}
