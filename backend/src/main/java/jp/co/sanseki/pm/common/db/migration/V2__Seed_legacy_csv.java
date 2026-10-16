package jp.co.sanseki.pm.common.db.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.co.sanseki.pm.common.util.Db2Timestamp;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 現行初期データ（db2/data/*.csv）をそのまま投入する。
 * CSV はリポジトリの db2/data をクラスパスに載せて直接読む（コピーを持たない）。
 */
public class V2__Seed_legacy_csv extends BaseJavaMigration {

    /** 外部キーの依存順。 */
    static final List<String> TABLES = List.of("item_mst", "bom", "stock", "work_order", "seq_ctl");

    @Override
    public void migrate(Context context) throws Exception {
        Connection con = context.getConnection();
        for (String table : TABLES) {
            load(con, table);
        }
    }

    private void load(Connection con, String table) throws IOException, SQLException {
        List<String> lines = readLines("db2/data/" + table + ".csv");
        String[] header = lines.get(0).split(",", -1);
        Map<String, String> types = columnTypes(con, table);

        String cols = String.join(",", header);
        String marks = String.join(",", java.util.Collections.nCopies(header.length, "?"));
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO " + table + " (" + cols + ") VALUES (" + marks + ")")) {
            for (String line : lines.subList(1, lines.size())) {
                if (line.isBlank()) {
                    continue;
                }
                String[] values = line.split(",", -1);
                for (int i = 0; i < header.length; i++) {
                    bind(ps, i + 1, types.get(header[i].toLowerCase(Locale.ROOT)), values[i]);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void bind(PreparedStatement ps, int index, String type, String value) throws SQLException {
        switch (type) {
            case "timestamp without time zone" -> ps.setTimestamp(index, Timestamp.valueOf(Db2Timestamp.parse(value)));
            case "date" -> ps.setDate(index, Date.valueOf(LocalDate.parse(value)));
            case "numeric" -> ps.setBigDecimal(index, new BigDecimal(value));
            case "integer" -> ps.setInt(index, Integer.parseInt(value));
            default -> ps.setString(index, value);
        }
    }

    private static Map<String, String> columnTypes(Connection con, String table) throws SQLException {
        Map<String, String> types = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT column_name, data_type FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    types.put(rs.getString(1), rs.getString(2));
                }
            }
        }
        return types;
    }

    private static List<String> readLines(String resource) throws IOException {
        try (InputStream in = V2__Seed_legacy_csv.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("初期データが見つかりません: " + resource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.startsWith("\uFEFF") ? line.substring(1) : line);
            }
            return lines;
        }
    }
}
