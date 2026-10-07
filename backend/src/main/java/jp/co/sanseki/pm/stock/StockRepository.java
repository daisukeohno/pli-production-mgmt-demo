package jp.co.sanseki.pm.stock;

import java.util.Optional;

import jp.co.sanseki.pm.common.Chars;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class StockRepository {

    static final RowMapper<Stock> ROW_MAPPER = (rs, i) -> new Stock(
            Chars.trimPadding(rs.getString("ITEM_CD")),
            rs.getBigDecimal("STOCK_QTY"),
            rs.getTimestamp("UPD_TMS").toLocalDateTime());

    private final JdbcClient jdbc;

    public StockRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Stock> findByItemCd(String itemCd) {
        return jdbc.sql("SELECT ITEM_CD, STOCK_QTY, UPD_TMS FROM SANSEKI.STOCK WHERE ITEM_CD = :itemCd")
                .param("itemCd", itemCd)
                .query(ROW_MAPPER)
                .optional();
    }
}
