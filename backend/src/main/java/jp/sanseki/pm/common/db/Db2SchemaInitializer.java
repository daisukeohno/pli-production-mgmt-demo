package jp.sanseki.pm.common.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import jp.sanseki.pm.common.util.Db2Timestamp;
import jp.sanseki.pm.common.util.FixedChar;

/**
 * 現行資産の db2/ddl/*.sql をそのまま読み込んで開発用 RDBMS（H2 DB2 互換モード）へ投入し、
 * db2/data/*.csv の初期データをロードする。
 *
 * DDL は原本を変更せず、H2 が解釈できない DB2 固有句のみ機械的に変換する。
 * <ul>
 *   <li>{@code ) IN SANSEKI.TSnnn}（表スペース指定）→ 除去</li>
 *   <li>{@code ADD FOREIGN KEY 制約名 (列)} → {@code ADD CONSTRAINT 制約名 FOREIGN KEY (列)}</li>
 * </ul>
 */
@Component
public class Db2SchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(Db2SchemaInitializer.class);

    static final String SCHEMA = "SANSEKI";

    /** 外部キー依存順。 */
    static final List<String> DATA_LOAD_ORDER = List.of("ITEM_MST", "BOM", "STOCK", "WORK_ORDER", "SEQ_CTL");

    private static final Pattern LINE_COMMENT = Pattern.compile("--[^\\r\\n]*");
    private static final Pattern TABLESPACE = Pattern.compile("\\)\\s*IN\\s+[A-Z0-9_.]+\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMED_FK = Pattern.compile("ADD\\s+FOREIGN\\s+KEY\\s+(\\w+)\\s*\\(", Pattern.CASE_INSENSITIVE);

    private final JdbcTemplate jdbc;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public Db2SchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void initialize() {
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA);
        for (Resource ddl : resources("classpath:db2/ddl/*.sql")) {
            for (String stmt : toH2Statements(read(ddl))) {
                jdbc.execute(stmt);
            }
            log.info("DDL 投入: {}", ddl.getFilename());
        }
        for (String table : DATA_LOAD_ORDER) {
            Resource csv = resolver.getResource("classpath:db2/data/" + table.toLowerCase(Locale.ROOT) + ".csv");
            if (csv.exists()) {
                int n = loadCsv(table, read(csv));
                log.info("初期データ投入: {} {}件", table, n);
            }
        }
    }

    /** 開発・テスト用：全オブジェクトを削除して初期状態へ戻す。 */
    public void reset() {
        jdbc.execute("DROP ALL OBJECTS");
        initialize();
    }

    static List<String> toH2Statements(String ddl) {
        String body = LINE_COMMENT.matcher(ddl).replaceAll("");
        List<String> out = new ArrayList<>();
        for (String raw : body.split(";")) {
            String stmt = raw.trim();
            if (stmt.isEmpty()) {
                continue;
            }
            stmt = TABLESPACE.matcher(stmt).replaceAll(")");
            stmt = NAMED_FK.matcher(stmt).replaceAll("ADD CONSTRAINT $1 FOREIGN KEY (");
            out.add(stmt);
        }
        return out;
    }

    private int loadCsv(String table, String content) {
        List<String> lines = content.lines().filter(l -> !l.isBlank()).toList();
        if (lines.isEmpty()) {
            return 0;
        }
        String[] header = lines.get(0).replace("\uFEFF", "").split(",", -1);
        String qualified = SCHEMA + "." + table;

        Map<String, int[]> colMeta = new HashMap<>();
        jdbc.query("SELECT * FROM " + qualified + " WHERE 1 = 0", (ResultSetExtractor<Void>) rs -> {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                colMeta.put(md.getColumnName(i).toUpperCase(Locale.ROOT),
                        new int[] {md.getColumnType(i), md.getPrecision(i)});
            }
            return null;
        });

        String sql = "INSERT INTO " + qualified + " (" + String.join(",", header) + ") VALUES ("
                + String.join(",", java.util.Collections.nCopies(header.length, "?")) + ")";
        List<Object[]> batch = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] v = line.split(",", -1);
            if (v.length != header.length) {
                throw new IllegalStateException(table + " CSV の列数不一致: " + line);
            }
            Object[] row = new Object[v.length];
            for (int i = 0; i < v.length; i++) {
                int[] meta = colMeta.get(header[i].trim().toUpperCase(Locale.ROOT));
                if (meta == null) {
                    throw new IllegalStateException(table + " に列 " + header[i] + " がありません");
                }
                row[i] = convert(v[i], meta[0], meta[1]);
            }
            batch.add(row);
        }
        jdbc.batchUpdate(sql, batch);
        return batch.size();
    }

    private static Object convert(String v, int sqlType, int precision) {
        return switch (sqlType) {
            case Types.TIMESTAMP -> Timestamp.valueOf(Db2Timestamp.parse(v));
            case Types.DATE -> Date.valueOf(Db2Timestamp.parseDate(v));
            case Types.DECIMAL, Types.NUMERIC -> new BigDecimal(v.trim());
            case Types.INTEGER, Types.SMALLINT -> Integer.valueOf(v.trim());
            case Types.CHAR -> FixedChar.pad(v, precision);
            default -> v;
        };
    }

    private List<Resource> resources(String pattern) {
        try {
            Resource[] found = resolver.getResources(pattern);
            return Arrays.stream(found).sorted(Comparator.comparing(Resource::getFilename)).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Resource r) {
        try (InputStream in = r.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
