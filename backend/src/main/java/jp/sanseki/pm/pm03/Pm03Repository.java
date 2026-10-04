package jp.sanseki.pm.pm03;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import jp.sanseki.pm.common.util.FixedChar;

/**
 * PM03 が参照・更新する SANSEKI 各表（BOM / STOCK / WORK_ORDER / SEQ_CTL）への埋め込み SQL 相当。
 * ITEM_MST の製品検索は pm01 の {@link ItemMstRepository} をそのまま使う。
 */
@Repository
public class Pm03Repository {

    private static final String SEQ_NAME = "WORK_ORDER";
    private static final int ITEM_CD_LEN = 8;

    private final JdbcTemplate jdbc;

    public Pm03Repository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** DCL_BOM 相当（CHAR 列は末尾空白除去済）。 */
    public record Bom(String childItemCd, BigDecimal qtyPer, BigDecimal yieldRate) {
    }

    private static final RowMapper<Bom> BOM_MAPPER = Pm03Repository::mapBom;

    /** BOM の子部品構成を CHILD_ITEM_CD 順に取得。構成が無ければ空。 */
    public List<Bom> findBom(String parentItemCd) {
        return jdbc.query("SELECT CHILD_ITEM_CD, QTY_PER, YIELD_RATE FROM SANSEKI.BOM"
                        + " WHERE PARENT_ITEM_CD = ? ORDER BY CHILD_ITEM_CD",
                BOM_MAPPER, cd(parentItemCd));
    }

    /** STOCK の現在庫数量。行が無い品目は Optional.empty（呼び出し側で 0 として扱う）。 */
    public Optional<BigDecimal> stockQty(String itemCd) {
        return jdbc.query("SELECT STOCK_QTY FROM SANSEKI.STOCK WHERE ITEM_CD = ?",
                (rs, i) -> rs.getBigDecimal(1), cd(itemCd)).stream().findFirst();
    }

    /**
     * 製造指示番号の採番（design.md §2 ルール7：SEQ_CTL を UPDATE してから SELECT）。
     * UPDATE が行ロックを取るため、同時実行しても連番は重複しない。
     * SEQ_YY が現在の西暦下2桁と異なれば連番を 1 へ振り直す。
     *
     * @return 'W' + 西暦下2桁 + 4桁連番（例 W260003）
     */
    public String nextWorkOrderNo(String seqYy) {
        int updated = jdbc.update("UPDATE SANSEKI.SEQ_CTL"
                        + " SET SEQ_YY = ?, SEQ_NO = CASE WHEN SEQ_YY = ? THEN SEQ_NO + 1 ELSE 1 END"
                        + " WHERE SEQ_NAME = ?",
                seqYy, seqYy, SEQ_NAME);
        if (updated == 0) {
            // SEQ_CTL 行が無い場合の保険（初期データでは 1 行存在）
            jdbc.update("INSERT INTO SANSEKI.SEQ_CTL (SEQ_NAME, SEQ_YY, SEQ_NO) VALUES (?, ?, 1)",
                    SEQ_NAME, seqYy);
        }
        // UPDATE → COMMIT 前の SELECT は自トランザクションの更新値を参照する
        Integer seqNo = jdbc.queryForObject("SELECT SEQ_NO FROM SANSEKI.SEQ_CTL WHERE SEQ_NAME = ?",
                Integer.class, SEQ_NAME);
        return "W" + seqYy + String.format("%04d", seqNo);
    }

    /** WORK_ORDER への登録（STATUS='0'）。 */
    public void insertWorkOrder(String orderNo, String itemCd, BigDecimal orderQty, LocalDate dueDate,
            LocalDateTime now) {
        jdbc.update("INSERT INTO SANSEKI.WORK_ORDER"
                        + " (WORK_ORDER_NO, ITEM_CD, ORDER_QTY, DUE_DATE, STATUS, CRT_TMS)"
                        + " VALUES (?, ?, ?, ?, '0', ?)",
                FixedChar.pad(orderNo, 7), cd(itemCd), orderQty,
                Date.valueOf(dueDate), Timestamp.valueOf(now));
    }

    private static String cd(String itemCd) {
        return FixedChar.pad(itemCd, ITEM_CD_LEN);
    }

    private static Bom mapBom(ResultSet rs, int rowNum) throws SQLException {
        return new Bom(
                FixedChar.rtrim(rs.getString("CHILD_ITEM_CD")),
                rs.getBigDecimal("QTY_PER"),
                rs.getBigDecimal("YIELD_RATE"));
    }
}
