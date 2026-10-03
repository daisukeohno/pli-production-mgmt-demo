package jp.sanseki.pm.pm01;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import jp.sanseki.pm.common.util.FixedChar;

/** SANSEKI.ITEM_MST への埋め込み SQL 相当。 */
@Repository
public class ItemMstRepository {

    private static final String COLS = "ITEM_CD, ITEM_NAME, ITEM_KBN, STOCK_UNIT, DEL_FLG, UPD_TMS, CRT_TMS";

    private static final RowMapper<ItemMst> MAPPER = ItemMstRepository::map;

    private final JdbcTemplate jdbc;

    public ItemMstRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ItemMst> findActive(String itemCd) {
        return jdbc.query("SELECT " + COLS + " FROM SANSEKI.ITEM_MST WHERE ITEM_CD = ? AND DEL_FLG = '0'",
                MAPPER, cd(itemCd)).stream().findFirst();
    }

    /** 更新・削除直前の再読込（行ロック取得）。 */
    public Optional<ItemMst> findActiveForUpdate(String itemCd) {
        return jdbc.query("SELECT " + COLS + " FROM SANSEKI.ITEM_MST WHERE ITEM_CD = ? AND DEL_FLG = '0' FOR UPDATE",
                MAPPER, cd(itemCd)).stream().findFirst();
    }

    /** 論理削除済も含めた存在確認（主キー重複判定）。 */
    public boolean exists(String itemCd) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM SANSEKI.ITEM_MST WHERE ITEM_CD = ?",
                Integer.class, cd(itemCd));
        return n != null && n > 0;
    }

    public void insert(String itemCd, String itemName, String itemKbn, String stockUnit, LocalDateTime now) {
        jdbc.update("INSERT INTO SANSEKI.ITEM_MST (" + COLS + ") VALUES (?, ?, ?, ?, '0', ?, ?)",
                cd(itemCd),
                FixedChar.pad(itemName, ItemMst.ITEM_NAME_LEN),
                FixedChar.pad(itemKbn, ItemMst.ITEM_KBN_LEN),
                FixedChar.pad(stockUnit, ItemMst.STOCK_UNIT_LEN),
                Timestamp.valueOf(now), Timestamp.valueOf(now));
    }

    public int update(String itemCd, String itemName, String itemKbn, String stockUnit, LocalDateTime newTms) {
        return jdbc.update("UPDATE SANSEKI.ITEM_MST SET ITEM_NAME = ?, ITEM_KBN = ?, STOCK_UNIT = ?, UPD_TMS = ?"
                        + " WHERE ITEM_CD = ? AND DEL_FLG = '0'",
                FixedChar.pad(itemName, ItemMst.ITEM_NAME_LEN),
                FixedChar.pad(itemKbn, ItemMst.ITEM_KBN_LEN),
                FixedChar.pad(stockUnit, ItemMst.STOCK_UNIT_LEN),
                Timestamp.valueOf(newTms), cd(itemCd));
    }

    /** 論理削除（DEL_FLG='1'）。物理削除は行わない。 */
    public int logicalDelete(String itemCd, LocalDateTime newTms) {
        return jdbc.update("UPDATE SANSEKI.ITEM_MST SET DEL_FLG = '1', UPD_TMS = ? WHERE ITEM_CD = ? AND DEL_FLG = '0'",
                Timestamp.valueOf(newTms), cd(itemCd));
    }

    /** キーセット：ITEM_CD >= fromCd の有効品目を昇順に最大 limit 件。fromCd 空白は先頭から。 */
    public List<ItemMst> listFrom(String fromCd, int limit) {
        return jdbc.query("SELECT " + COLS + " FROM SANSEKI.ITEM_MST WHERE DEL_FLG = '0' AND ITEM_CD >= ?"
                + " ORDER BY ITEM_CD FETCH FIRST " + limit + " ROWS ONLY", MAPPER, cd(fromCd));
    }

    /** キーセット：ITEM_CD < beforeCd の有効品目を降順に最大 limit 件（PF7 前頁用）。 */
    public List<String> keysBefore(String beforeCd, int limit) {
        return jdbc.query("SELECT ITEM_CD FROM SANSEKI.ITEM_MST WHERE DEL_FLG = '0' AND ITEM_CD < ?"
                        + " ORDER BY ITEM_CD DESC FETCH FIRST " + limit + " ROWS ONLY",
                (rs, i) -> FixedChar.rtrim(rs.getString(1)), cd(beforeCd));
    }

    private static String cd(String itemCd) {
        return FixedChar.pad(itemCd, ItemMst.ITEM_CD_LEN);
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
