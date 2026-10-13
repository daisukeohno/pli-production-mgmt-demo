package jp.co.sanseki.pm.common.db;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import jp.co.sanseki.pm.common.lock.Db2Timestamp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 移行元資産から DB を構築する。
 * <ol>
 *   <li>db2/ddl/*.sql（DB2 DDL）を H2 で実行できる形へ最小限変換して実行（表スペース句・FK 名の位置のみ変換）</li>
 *   <li>db/app/*.sql（移行先で追加したテーブル。MSG_MST）を実行</li>
 *   <li>db2/data/*.csv を DDL の順序で投入（DB2 TIMESTAMP 文字列は TIMESTAMP へ変換）</li>
 *   <li>src/include/PMMSG.inc のメッセージ定義を MSG_MST へ投入</li>
 * </ol>
 */
@Component
public class LegacyDatabaseInitializer {

    public static final String SCHEMA = "SANSEKI";

    private static final Pattern TABLESPACE = Pattern.compile("(?is)\\)\\s*IN\\s+[A-Z0-9_.]+\\s*$");
    private static final Pattern NAMED_FK = Pattern.compile("(?i)ADD\\s+FOREIGN\\s+KEY\\s+(\\w+)\\s*\\(");
    private static final Pattern MSG_INIT = Pattern.compile("INIT\\('(M\\d{3})','([^']*)'\\)");

    private final JdbcTemplate jdbc;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final String legacyLocation;
    private final List<Runnable> resetListeners = new CopyOnWriteArrayList<>();

    public LegacyDatabaseInitializer(JdbcTemplate jdbc,
                                     @Value("${pm.db.legacy-location:classpath:legacy}") String legacyLocation) {
        this.jdbc = jdbc;
        this.legacyLocation = legacyLocation;
    }

    @PostConstruct
    void initialize() {
        rebuild();
    }

    public void addResetListener(Runnable listener) {
        resetListeners.add(listener);
    }

    /** DB を初期状態（DDL + 初期データ）へ戻す。シナリオテストの前提条件づくりにも使う。 */
    public synchronized void reset() {
        rebuild();
        resetListeners.forEach(Runnable::run);
    }

    private void rebuild() {
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE SCHEMA " + SCHEMA);
        List<String> tables = new ArrayList<>();
        for (Resource ddl : resources(legacyLocation + "/db2/ddl/*.sql")) {
            toPortableStatements(read(ddl)).forEach(jdbc::execute);
            tables.add(tableNameOf(ddl.getFilename()));
        }
        for (Resource ddl : resources("classpath:db/app/*.sql")) {
            toPortableStatements(read(ddl)).forEach(jdbc::execute);
        }
        for (String table : tables) {
            Resource csv = resolver.getResource(legacyLocation + "/db2/data/" + table.toLowerCase(Locale.ROOT) + ".csv");
            if (csv.exists()) {
                loadCsv(table, read(csv));
            }
        }
        loadMessages(read(resolver.getResource(legacyLocation + "/include/PMMSG.inc")));
    }

    /** DB2 DDL → H2（MODE=DB2）向け。コメント除去・文分割・表スペース句除去・FK 制約名の位置変換のみ行う。 */
    public static List<String> toPortableStatements(String db2Ddl) {
        StringBuilder sb = new StringBuilder();
        for (String line : db2Ddl.split("\\R")) {
            int comment = line.indexOf("--");
            sb.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }
        List<String> statements = new ArrayList<>();
        for (String raw : sb.toString().split(";")) {
            String stmt = raw.trim();
            if (stmt.isEmpty()) {
                continue;
            }
            stmt = TABLESPACE.matcher(stmt).replaceAll(")");
            stmt = NAMED_FK.matcher(stmt).replaceAll("ADD CONSTRAINT $1 FOREIGN KEY (");
            statements.add(stmt);
        }
        return statements;
    }

    /** PMMSG.inc の INIT('Mnnn','本文') を抽出する（本文の右側空白は除去）。 */
    public static List<String[]> parseMessages(String pmmsg) {
        List<String[]> messages = new ArrayList<>();
        Matcher m = MSG_INIT.matcher(pmmsg);
        while (m.find()) {
            messages.add(new String[] {m.group(1), m.group(2).stripTrailing()});
        }
        return messages;
    }

    private void loadCsv(String table, String content) {
        List<String> lines = content.lines().filter(l -> !l.isBlank()).toList();
        String[] columns = lines.get(0).trim().split(",");
        String sql = "INSERT INTO " + SCHEMA + "." + table + " (" + String.join(",", columns) + ") VALUES ("
                + String.join(",", java.util.Collections.nCopies(columns.length, "?")) + ")";
        List<Object[]> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] values = line.split(",", -1);
            if (values.length != columns.length) {
                throw new IllegalStateException(table + ".csv の列数が不正です: " + line);
            }
            rows.add(Arrays.stream(values).map(LegacyDatabaseInitializer::toSqlValue).toArray());
        }
        jdbc.batchUpdate(sql, rows);
    }

    private static Object toSqlValue(String value) {
        if (value.isEmpty()) {
            return null;
        }
        var tms = Db2Timestamp.parseOrNull(value);
        return tms != null ? Timestamp.valueOf(tms) : value;
    }

    private void loadMessages(String pmmsg) {
        List<Object[]> rows = new ArrayList<>(parseMessages(pmmsg));
        jdbc.batchUpdate("INSERT INTO " + SCHEMA + ".MSG_MST (MSG_ID, MSG_TEXT) VALUES (?, ?)", rows);
    }

    private static String tableNameOf(String ddlFileName) {
        return ddlFileName.replaceFirst("^\\d+_", "").replaceFirst("\\.sql$", "").toUpperCase(Locale.ROOT);
    }

    private List<Resource> resources(String pattern) {
        try {
            return Arrays.stream(resolver.getResources(pattern))
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
