package jp.sanseki.pm.pm02;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jp.sanseki.pm.common.util.FixedChar;

/** SANSEKI.STOCK（と品目名・区分を得るための SANSEKI.ITEM_MST）への埋め込み SQL 相当。 */
@Repository
public class StockRepository {

    private final JdbcTemplate jdbc;

    public StockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 有効品目（DEL_FLG='0'）と在庫数量。STOCK 行が無い場合 stockQty は null。 */
    public Optional<StockItem> findActiveItem(String itemCd) {
        return jdbc.query("SELECT I.ITEM_CD, I.ITEM_NAME, I.ITEM_KBN, S.STOCK_QTY"
                        + " FROM SANSEKI.ITEM_MST I LEFT OUTER JOIN SANSEKI.STOCK S ON S.ITEM_CD = I.ITEM_CD"
                        + " WHERE I.ITEM_CD = ? AND I.DEL_FLG = '0'",
                (rs, i) -> new StockItem(
                        FixedChar.rtrim(rs.getString("ITEM_CD")),
                        FixedChar.rtrim(rs.getString("ITEM_NAME")),
                        FixedChar.rtrim(rs.getString("ITEM_KBN")),
                        qty(rs.getBigDecimal("STOCK_QTY"))),
                cd(itemCd)).stream().findFirst();
    }

    /** 入出庫直前の再読込（STOCK 行ロック取得）。 */
    public Optional<StockRow> findForUpdate(String itemCd) {
        return jdbc.query("SELECT STOCK_QTY, UPD_TMS FROM SANSEKI.STOCK WHERE ITEM_CD = ? FOR UPDATE",
                (rs, i) -> new StockRow(qty(rs.getBigDecimal("STOCK_QTY")), rs.getTimestamp("UPD_TMS").toLocalDateTime()),
                cd(itemCd)).stream().findFirst();
    }

    public int update(String itemCd, BigDecimal stockQty, LocalDateTime newTms) {
        return jdbc.update("UPDATE SANSEKI.STOCK SET STOCK_QTY = ?, UPD_TMS = ? WHERE ITEM_CD = ?",
                stockQty, Timestamp.valueOf(newTms), cd(itemCd));
    }

    public void insert(String itemCd, BigDecimal stockQty, LocalDateTime now) {
        jdbc.update("INSERT INTO SANSEKI.STOCK (ITEM_CD, STOCK_QTY, UPD_TMS) VALUES (?, ?, ?)",
                cd(itemCd), stockQty, Timestamp.valueOf(now));
    }

    /** DEC(9,0)。H2 が返す指数表記（例 1.2E+2）を整数表記にそろえる。 */
    private static BigDecimal qty(BigDecimal v) {
        return v == null ? null : v.setScale(0);
    }

    private static String cd(String itemCd) {
        return FixedChar.pad(itemCd, StockItem.ITEM_CD_LEN);
    }

    /** DCLSTOCK 相当（ITEM_CD を除く）。 */
    public record StockRow(BigDecimal stockQty, LocalDateTime updTms) {
    }
}
