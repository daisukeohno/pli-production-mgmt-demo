package jp.co.sanseki.pm.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.co.sanseki.pm.common.Db2Timestamp;
import jp.co.sanseki.pm.item.ItemMst;
import jp.co.sanseki.pm.item.ItemRepository;
import jp.co.sanseki.pm.stock.StockRepository;
import jp.co.sanseki.pm.workorder.Bom;
import jp.co.sanseki.pm.workorder.BomRepository;
import jp.co.sanseki.pm.workorder.SeqCtl;
import jp.co.sanseki.pm.workorder.SeqCtlRepository;
import jp.co.sanseki.pm.workorder.WorkOrder;
import jp.co.sanseki.pm.workorder.WorkOrderRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flyway で SANSEKI スキーマの 5 テーブルと初期データが作られることの統合テスト。
 * 同じテストを H2（{@link H2DatabaseIntegrationTest}）と PostgreSQL（{@link PostgresDatabaseIntegrationTest}）で実行する。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractDatabaseIntegrationTest {

    static final List<String> TABLES = List.of("ITEM_MST", "BOM", "STOCK", "WORK_ORDER", "SEQ_CTL");

    @Autowired
    Flyway flyway;
    @Autowired
    JdbcClient jdbc;
    @Autowired
    TestRestTemplate rest;
    @Autowired
    ItemRepository itemRepository;
    @Autowired
    StockRepository stockRepository;
    @Autowired
    BomRepository bomRepository;
    @Autowired
    WorkOrderRepository workOrderRepository;
    @Autowired
    SeqCtlRepository seqCtlRepository;

    @Test
    void flywayAppliedBothMigrations() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).extracting(m -> m.getVersion().getVersion()).containsExactly("1", "2");
        assertThat(applied).extracting(MigrationInfo::getState).containsOnly(MigrationState.SUCCESS);
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void fiveTablesExistInSansekiSchema() {
        List<String> tables = jdbc.sql("""
                SELECT UPPER(TABLE_NAME) FROM INFORMATION_SCHEMA.TABLES
                 WHERE UPPER(TABLE_SCHEMA) = 'SANSEKI'
                 ORDER BY 1
                """).query(String.class).list();
        assertThat(tables).containsExactlyInAnyOrderElementsOf(TABLES);
    }

    record Col(String type, Integer charLen, Integer precision, Integer scale, String nullable, String defaultValue) {
    }

    private Col column(String table, String column) {
        return jdbc.sql("""
                SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE, DATETIME_PRECISION,
                       IS_NULLABLE, COLUMN_DEFAULT
                  FROM INFORMATION_SCHEMA.COLUMNS
                 WHERE UPPER(TABLE_SCHEMA) = 'SANSEKI' AND UPPER(TABLE_NAME) = :t AND UPPER(COLUMN_NAME) = :c
                """)
                .param("t", table).param("c", column)
                .query((rs, i) -> {
                    String type = rs.getString("DATA_TYPE").toLowerCase(Locale.ROOT);
                    Integer precision = type.startsWith("timestamp")
                            ? (Integer) rs.getObject("DATETIME_PRECISION", Integer.class)
                            : (Integer) rs.getObject("NUMERIC_PRECISION", Integer.class);
                    return new Col(normalize(type), rs.getObject("CHARACTER_MAXIMUM_LENGTH", Integer.class),
                            precision, rs.getObject("NUMERIC_SCALE", Integer.class),
                            rs.getString("IS_NULLABLE"), rs.getString("COLUMN_DEFAULT"));
                })
                .single();
    }

    private static String normalize(String type) {
        if (type.startsWith("timestamp")) {
            return "timestamp";
        }
        if (type.equals("decimal")) {
            return "numeric";
        }
        return type;
    }

    private void assertChar(String table, String column, int length) {
        Col c = column(table, column);
        assertThat(c.type()).as(table + "." + column).isEqualTo("character");
        assertThat(c.charLen()).as(table + "." + column).isEqualTo(length);
        assertThat(c.nullable()).as(table + "." + column).isEqualTo("NO");
    }

    private void assertDecimal(String table, String column, int precision, int scale) {
        Col c = column(table, column);
        assertThat(c.type()).as(table + "." + column).isEqualTo("numeric");
        assertThat(c.precision()).as(table + "." + column).isEqualTo(precision);
        assertThat(c.scale()).as(table + "." + column).isEqualTo(scale);
        assertThat(c.nullable()).as(table + "." + column).isEqualTo("NO");
    }

    private void assertTimestamp(String table, String column) {
        Col c = column(table, column);
        assertThat(c.type()).as(table + "." + column).isEqualTo("timestamp");
        assertThat(c.precision()).as(table + "." + column + " はマイクロ秒精度").isEqualTo(6);
        assertThat(c.nullable()).as(table + "." + column).isEqualTo("NO");
    }

    @Test
    void columnTypesMatchDb2Ddl() {
        assertChar("ITEM_MST", "ITEM_CD", 8);
        assertChar("ITEM_MST", "ITEM_NAME", 20);
        assertChar("ITEM_MST", "ITEM_KBN", 1);
        assertChar("ITEM_MST", "STOCK_UNIT", 4);
        assertChar("ITEM_MST", "DEL_FLG", 1);
        assertThat(column("ITEM_MST", "DEL_FLG").defaultValue()).contains("'0'");
        assertTimestamp("ITEM_MST", "UPD_TMS");
        assertTimestamp("ITEM_MST", "CRT_TMS");

        assertChar("BOM", "PARENT_ITEM_CD", 8);
        assertChar("BOM", "CHILD_ITEM_CD", 8);
        assertDecimal("BOM", "QTY_PER", 7, 2);
        assertDecimal("BOM", "YIELD_RATE", 5, 2);
        assertTimestamp("BOM", "UPD_TMS");

        assertChar("STOCK", "ITEM_CD", 8);
        assertDecimal("STOCK", "STOCK_QTY", 9, 0);
        assertTimestamp("STOCK", "UPD_TMS");

        assertChar("WORK_ORDER", "WORK_ORDER_NO", 7);
        assertChar("WORK_ORDER", "ITEM_CD", 8);
        assertDecimal("WORK_ORDER", "ORDER_QTY", 9, 0);
        assertThat(column("WORK_ORDER", "DUE_DATE").type()).isEqualTo("date");
        assertChar("WORK_ORDER", "STATUS", 1);
        assertThat(column("WORK_ORDER", "STATUS").defaultValue()).contains("'0'");
        assertTimestamp("WORK_ORDER", "CRT_TMS");

        assertChar("SEQ_CTL", "SEQ_NAME", 10);
        assertChar("SEQ_CTL", "SEQ_YY", 2);
        assertThat(column("SEQ_CTL", "SEQ_NO").type()).isEqualTo("integer");
    }

    @Test
    void charColumnsArePaddedToFixedLength() {
        // CHAR(n) は固定長：DB 上は n 文字に空白パディングされる（Repository では末尾空白を除いて扱う）
        assertThat(jdbc.sql("SELECT CAST(ITEM_NAME AS VARCHAR(40)) FROM SANSEKI.ITEM_MST WHERE ITEM_CD = '20000011'")
                .query(String.class).single()).isIn("主軸アセンブリ", "主軸アセンブリ             ");
        assertThat(jdbc.sql("SELECT OCTET_LENGTH(SEQ_NAME) FROM SANSEKI.SEQ_CTL").query(Integer.class).single())
                .isEqualTo(10);
        // CHAR 比較は末尾空白を無視する（DB2 と同じ）
        assertThat(jdbc.sql("SELECT COUNT(*) FROM SANSEKI.SEQ_CTL WHERE SEQ_NAME = 'WORK_ORDER'")
                .query(Integer.class).single()).isEqualTo(1);
    }

    static long csvRows(String csv) throws IOException {
        try (InputStream in = AbstractDatabaseIntegrationTest.class.getClassLoader()
                .getResourceAsStream("db/data/" + csv)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().skip(1).filter(l -> !l.isBlank()).count();
        }
    }

    @Test
    void initialDataRowCountsMatchCsv() throws IOException {
        for (String table : TABLES) {
            long count = jdbc.sql("SELECT COUNT(*) FROM SANSEKI." + table).query(Long.class).single();
            assertThat(count).as(table).isEqualTo(csvRows(table.toLowerCase(Locale.ROOT) + ".csv"));
        }
        assertThat(csvRows("item_mst.csv")).isEqualTo(15);
        assertThat(csvRows("bom.csv")).isEqualTo(9);
        assertThat(csvRows("stock.csv")).isEqualTo(15);
        assertThat(csvRows("work_order.csv")).isEqualTo(2);
        assertThat(csvRows("seq_ctl.csv")).isEqualTo(1);
    }

    @Test
    void initialDataValuesAreLoadedWithTypes() {
        ItemMst item = itemRepository.findByItemCd("10000014").orElseThrow();
        assertThat(item.itemName()).isEqualTo("精密減速機ユニットA型");
        assertThat(item.itemKbn()).isEqualTo(ItemMst.KBN_PRODUCT);
        assertThat(item.stockUnit()).isEqualTo("台");
        assertThat(item.delFlg()).isEqualTo(ItemMst.DEL_FLG_ACTIVE);
        assertThat(Db2Timestamp.format(item.updTms())).isEqualTo("2026-09-01-09.00.00.000000");
        assertThat(item.crtTms()).isEqualTo(LocalDateTime.of(2015, 4, 1, 9, 0));
        assertThat(itemRepository.findByItemCd("90000010").orElseThrow().stockUnit()).isEqualTo("KG");
        assertThat(itemRepository.countActive()).isEqualTo(15);

        assertThat(stockRepository.findByItemCd("20000035").orElseThrow().stockQty()).isEqualByComparingTo("60");

        List<Bom> bom = bomRepository.findByParentItemCd("10000021");
        assertThat(bom).extracting(Bom::childItemCd)
                .containsExactly("20000011", "20000042", "20000066", "20000080", "20000097");
        Bom gearB = bom.get(1);
        assertThat(gearB.qtyPer()).isEqualTo(new BigDecimal("2.00"));
        assertThat(gearB.yieldRate()).isEqualTo(new BigDecimal("93.00"));

        WorkOrder wo = workOrderRepository.findByWorkOrderNo("W260002").orElseThrow();
        assertThat(wo.itemCd()).isEqualTo("10000021");
        assertThat(wo.orderQty()).isEqualByComparingTo("30");
        assertThat(wo.dueDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(wo.status()).isEqualTo("0");
        assertThat(wo.crtTms()).isEqualTo(LocalDateTime.of(2026, 9, 25, 14, 30));

        assertThat(seqCtlRepository.findBySeqName(SeqCtl.WORK_ORDER)).contains(new SeqCtl("WORK_ORDER", "26", 2));
    }

    @Test
    @Transactional
    void timestampKeepsMicroseconds() {
        jdbc.sql("UPDATE SANSEKI.ITEM_MST SET UPD_TMS = :t WHERE ITEM_CD = '20000103'")
                .param("t", Db2Timestamp.parse("2026-10-07-18.30.15.123456"))
                .update();
        assertThat(Db2Timestamp.format(itemRepository.findByItemCd("20000103").orElseThrow().updTms()))
                .isEqualTo("2026-10-07-18.30.15.123456");
    }

    @Test
    @Transactional
    void defaultsAndConstraintsAreApplied() {
        jdbc.sql("""
                INSERT INTO SANSEKI.ITEM_MST (ITEM_CD, ITEM_NAME, ITEM_KBN, STOCK_UNIT, UPD_TMS, CRT_TMS)
                VALUES ('20000110', 'テスト部品', '2', '個', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).update();
        assertThat(itemRepository.findByItemCd("20000110").orElseThrow().delFlg()).isEqualTo("0");

        // 主キー重複
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO SANSEKI.ITEM_MST (ITEM_CD, ITEM_NAME, ITEM_KBN, STOCK_UNIT, UPD_TMS, CRT_TMS)
                VALUES ('10000014', 'x', '1', '台', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void foreignKeysAreEnforced() {
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO SANSEKI.STOCK (ITEM_CD, STOCK_QTY, UPD_TMS) VALUES ('99999999', 1, CURRENT_TIMESTAMP)
                """).update()).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO SANSEKI.BOM (PARENT_ITEM_CD, CHILD_ITEM_CD, QTY_PER, YIELD_RATE, UPD_TMS)
                VALUES ('10000014', '99999999', 1, 100, CURRENT_TIMESTAMP)
                """).update()).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO SANSEKI.WORK_ORDER (WORK_ORDER_NO, ITEM_CD, ORDER_QTY, DUE_DATE, CRT_TMS)
                VALUES ('W269999', '99999999', 1, DATE '2026-12-01', CURRENT_TIMESTAMP)
                """).update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void decimalPrecisionIsEnforced() {
        // DEC(5,2) の YIELD_RATE に 1000.00 は入らない
        assertThatThrownBy(() -> jdbc.sql("""
                UPDATE SANSEKI.BOM SET YIELD_RATE = 1000.00
                 WHERE PARENT_ITEM_CD = '10000014' AND CHILD_ITEM_CD = '20000011'
                """).update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void healthCheckReportsUpWithTableCounts() {
        ResponseEntity<Map> res = rest.getForEntity("/actuator/health", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = res.getBody();
        assertThat(body).containsEntry("status", "UP");
        Map<String, Object> components = (Map<String, Object>) body.get("components");
        assertThat((Map<String, Object>) components.get("db")).containsEntry("status", "UP");
        Map<String, Object> sanseki = (Map<String, Object>) components.get("sanseki");
        assertThat(sanseki).containsEntry("status", "UP");
        Map<String, Object> details = (Map<String, Object>) sanseki.get("details");
        assertThat((Map<String, Object>) details.get("rowCounts")).isEqualTo(Map.of(
                "ITEM_MST", 15, "BOM", 9, "STOCK", 15, "WORK_ORDER", 2, "SEQ_CTL", 1));
    }

    @Test
    void unknownApiReturnsCommonErrorShape() {
        ResponseEntity<Map> res = rest.getForEntity("/api/no-such-api", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).containsOnlyKeys("msgId", "message", "field");
    }
}
