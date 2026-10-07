package db.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jp.co.sanseki.pm.common.Db2Timestamp;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * db2/data/*.csv（UTF-8、先頭行は列名）を初期データとして投入する。
 * CSV はコピーせず、pom.xml でクラスパスの db/data/ に載せたものを読む（正は db2/data）。
 * TIMESTAMP は DB2 形式 YYYY-MM-DD-HH.MM.SS.ffffff、DATE は YYYY-MM-DD。
 */
public class V2__LoadInitialData extends BaseJavaMigration {

    enum Type { CHAR, DEC, INT, DATE, TIMESTAMP }

    record Column(String name, Type type) {
    }

    record Table(String name, String csv, List<Column> columns) {
    }

    static final List<Table> TABLES = List.of(
            new Table("ITEM_MST", "item_mst.csv", List.of(
                    new Column("ITEM_CD", Type.CHAR), new Column("ITEM_NAME", Type.CHAR),
                    new Column("ITEM_KBN", Type.CHAR), new Column("STOCK_UNIT", Type.CHAR),
                    new Column("DEL_FLG", Type.CHAR), new Column("UPD_TMS", Type.TIMESTAMP),
                    new Column("CRT_TMS", Type.TIMESTAMP))),
            new Table("BOM", "bom.csv", List.of(
                    new Column("PARENT_ITEM_CD", Type.CHAR), new Column("CHILD_ITEM_CD", Type.CHAR),
                    new Column("QTY_PER", Type.DEC), new Column("YIELD_RATE", Type.DEC),
                    new Column("UPD_TMS", Type.TIMESTAMP))),
            new Table("STOCK", "stock.csv", List.of(
                    new Column("ITEM_CD", Type.CHAR), new Column("STOCK_QTY", Type.DEC),
                    new Column("UPD_TMS", Type.TIMESTAMP))),
            new Table("WORK_ORDER", "work_order.csv", List.of(
                    new Column("WORK_ORDER_NO", Type.CHAR), new Column("ITEM_CD", Type.CHAR),
                    new Column("ORDER_QTY", Type.DEC), new Column("DUE_DATE", Type.DATE),
                    new Column("STATUS", Type.CHAR), new Column("CRT_TMS", Type.TIMESTAMP))),
            new Table("SEQ_CTL", "seq_ctl.csv", List.of(
                    new Column("SEQ_NAME", Type.CHAR), new Column("SEQ_YY", Type.CHAR),
                    new Column("SEQ_NO", Type.INT))));

    @Override
    public void migrate(Context context) throws Exception {
        Connection con = context.getConnection();
        for (Table table : TABLES) {
            load(con, table);
        }
    }

    private static void load(Connection con, Table table) throws IOException, SQLException {
        List<String[]> rows = readCsv("db/data/" + table.csv());
        List<String> header = List.of(rows.get(0));
        List<String> expected = table.columns().stream().map(Column::name).toList();
        if (!header.equals(expected)) {
            throw new IllegalStateException(table.csv() + " の列が DDL と一致しません: " + header + " != " + expected);
        }
        String sql = "INSERT INTO SANSEKI." + table.name()
                + " (" + String.join(", ", expected) + ") VALUES ("
                + String.join(", ", expected.stream().map(c -> "?").toList()) + ")";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (String[] row : rows.subList(1, rows.size())) {
                if (row.length != expected.size()) {
                    throw new IllegalStateException(table.csv() + " の列数が不正です: " + String.join(",", row));
                }
                for (int i = 0; i < row.length; i++) {
                    bind(ps, i + 1, table.columns().get(i).type(), row[i]);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void bind(PreparedStatement ps, int index, Type type, String value) throws SQLException {
        switch (type) {
            case CHAR -> ps.setString(index, value);
            case DEC -> ps.setBigDecimal(index, new BigDecimal(value));
            case INT -> ps.setInt(index, Integer.parseInt(value));
            case DATE -> ps.setDate(index, Date.valueOf(LocalDate.parse(value)));
            case TIMESTAMP -> ps.setTimestamp(index, Timestamp.valueOf(Db2Timestamp.parse(value)));
        }
    }

    /** 初期データ CSV は引用符・埋め込みカンマを含まない単純な形式（含まれていたら失敗させる）。 */
    static List<String[]> readCsv(String resource) throws IOException {
        InputStream in = V2__LoadInitialData.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("初期データが見つかりません: " + resource);
        }
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                if (line.isBlank()) {
                    continue;
                }
                if (line.indexOf('"') >= 0) {
                    throw new IllegalStateException(resource + " に引用符付きの値があります: " + line);
                }
                rows.add(line.split(",", -1));
            }
        }
        return rows;
    }
}
