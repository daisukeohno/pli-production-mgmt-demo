package jp.co.sanseki.pm.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.sanseki.pm.common.db.LegacyDatabaseInitializer;
import jp.co.sanseki.pm.common.lock.Db2Timestamp;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.yaml.snakeyaml.Yaml;

/**
 * test/scenarios/pm01/*.yaml を実 HTTP（起動したサーバー）に対して実行する業務シナリオテスト。
 *
 * <p>各シナリオの前に DB を初期データ（db2/ddl + db2/data）へ戻し、steps の API 応答と db の最終状態を検証する。
 * 実行結果は target/scenario-report.md に出力する。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScenarioTest {

    private static final Pattern VAR = Pattern.compile("\\$\\{(\\w+)}");
    private static final List<String> REPORT = new ArrayList<>();

    @LocalServerPort
    int port;

    @Autowired
    LegacyDatabaseInitializer initializer;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @TestFactory
    Stream<DynamicNode> pm01() throws IOException {
        return scenarios("pm01");
    }

    private Stream<DynamicNode> scenarios(String screen) throws IOException {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:scenarios/" + screen + "/*.yaml");
        assertThat(files).as("scenarios/" + screen).isNotEmpty();
        return Arrays.stream(files)
                .sorted(Comparator.comparing(Resource::getFilename))
                .map(this::scenario);
    }

    @SuppressWarnings("unchecked")
    private DynamicNode scenario(Resource file) {
        Map<String, Object> sc;
        try {
            sc = new Yaml().load(file.getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        String title = sc.get("id") + " " + sc.get("title");
        return DynamicTest.dynamicTest(title, () -> {
            try {
                run(sc);
                REPORT.add("| " + sc.get("id") + " | " + sc.get("title") + " | " + file.getFilename() + " | PASS |");
            } catch (Throwable t) {
                REPORT.add("| " + sc.get("id") + " | " + sc.get("title") + " | " + file.getFilename() + " | **FAIL** |");
                throw t;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void run(Map<String, Object> sc) throws Exception {
        initializer.reset();
        for (String sql : (List<String>) sc.getOrDefault("given", List.of())) {
            jdbc.execute(sql);
        }
        Map<String, String> vars = new HashMap<>();
        for (Map<String, Object> step : (List<Map<String, Object>>) sc.getOrDefault("steps", List.of())) {
            runStep(step, vars);
        }
        for (Map<String, Object> check : (List<Map<String, Object>>) sc.getOrDefault("db", List.of())) {
            checkDb(check, vars);
        }
    }

    @SuppressWarnings("unchecked")
    private void runStep(Map<String, Object> step, Map<String, String> vars) throws Exception {
        String name = String.valueOf(step.get("name"));
        Map<String, Object> req = (Map<String, Object>) step.get("request");
        String method = String.valueOf(req.get("method"));
        StringBuilder uri = new StringBuilder("http://localhost:" + port + subst(String.valueOf(req.get("path")), vars));
        Map<String, Object> query = (Map<String, Object>) req.getOrDefault("query", Map.of());
        String sep = uri.indexOf("?") < 0 ? "?" : "&";
        for (var e : query.entrySet()) {
            uri.append(sep).append(e.getKey()).append('=')
                    .append(URLEncoder.encode(subst(String.valueOf(e.getValue()), vars), StandardCharsets.UTF_8));
            sep = "&";
        }
        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.noBody();
        if (req.containsKey("body")) {
            body = HttpRequest.BodyPublishers.ofString(json.writeValueAsString(substAll(req.get("body"), vars)), StandardCharsets.UTF_8);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri.toString()))
                .method(method, body)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode node = res.body().isEmpty() ? json.nullNode() : json.readTree(res.body());
        String ctx = "[" + name + "] " + method + " " + uri + " -> " + res.statusCode() + " " + res.body();

        Map<String, Object> expect = (Map<String, Object>) step.getOrDefault("expect", Map.of());
        if (expect.containsKey("status")) {
            assertThat(res.statusCode()).as(ctx).isEqualTo(((Number) expect.get("status")).intValue());
        }
        for (var e : ((Map<String, Object>) expect.getOrDefault("json", Map.of())).entrySet()) {
            assertMatches(ctx + " / " + e.getKey(), select(node, e.getKey()), substAll(e.getValue(), vars));
        }
        for (String absent : (List<String>) expect.getOrDefault("absent", List.of())) {
            JsonNode v = select(node, absent);
            assertThat(v == null || v.isNull() || v.isMissingNode()).as(ctx + " / absent " + absent).isTrue();
        }
        for (var e : ((Map<String, String>) step.getOrDefault("save", Map.of())).entrySet()) {
            JsonNode v = select(node, e.getValue());
            assertThat(v).as(ctx + " / save " + e.getValue()).isNotNull();
            vars.put(e.getKey(), v.asText());
        }
    }

    @SuppressWarnings("unchecked")
    private void checkDb(Map<String, Object> check, Map<String, String> vars) {
        String sql = subst(String.valueOf(check.get("sql")), vars);
        List<Map<String, Object>> actual = jdbc.queryForList(sql);
        List<Map<String, Object>> expected = (List<Map<String, Object>>) substAll(check.get("rows"), vars);
        assertThat(actual).as("db: " + sql + " -> " + actual).hasSize(expected.size());
        for (int i = 0; i < expected.size(); i++) {
            for (var e : expected.get(i).entrySet()) {
                Object value = actual.get(i).get(e.getKey());
                String text = value instanceof Timestamp ts ? Db2Timestamp.format(ts.toLocalDateTime())
                        : value == null ? null : value.toString().stripTrailing();
                assertMatches("db: " + sql + " row " + i + " " + e.getKey() + " actual=" + actual.get(i),
                        text == null ? json.nullNode() : json.getNodeFactory().textNode(text), e.getValue());
            }
        }
    }

    /** 期待値の書式: 値そのもの（等価）／"=~ 正規表現"／"!= 値"／配列（順序どおり一致）。 */
    private void assertMatches(String ctx, JsonNode actual, Object expected) {
        if (actual == null) {
            fail(ctx + ": 項目がありません");
        }
        if (expected instanceof List<?> list) {
            assertThat(actual.isArray()).as(ctx + " is array: " + actual).isTrue();
            assertThat(actual.size()).as(ctx + " size: " + actual).isEqualTo(list.size());
            for (int i = 0; i < list.size(); i++) {
                assertMatches(ctx + "[" + i + "]", actual.get(i), list.get(i));
            }
            return;
        }
        if (expected == null) {
            assertThat(actual.isNull()).as(ctx + ": " + actual).isTrue();
            return;
        }
        String exp = String.valueOf(expected);
        String act = actual.isValueNode() ? actual.asText() : actual.toString();
        if (exp.startsWith("=~ ")) {
            assertThat(act).as(ctx).matches(exp.substring(3));
        } else if (exp.startsWith("!= ")) {
            assertThat(act).as(ctx).isNotEqualTo(exp.substring(3));
        } else {
            assertThat(act).as(ctx).isEqualTo(exp);
        }
    }

    /** "a.b[0].c"、"items[*].itemCd"（配列の各要素から抽出）、"items.size"（要素数）。 */
    private JsonNode select(JsonNode root, String path) {
        List<JsonNode> current = new ArrayList<>(List.of(root));
        boolean collect = false;
        for (String token : path.split("\\.")) {
            Matcher m = Pattern.compile("^(\\w*)((?:\\[(?:\\d+|\\*)])*)$").matcher(token);
            if (!m.matches()) {
                throw new IllegalArgumentException("path: " + path);
            }
            List<JsonNode> next = new ArrayList<>();
            for (JsonNode n : current) {
                if ("size".equals(m.group(1)) && m.group(2).isEmpty() && n.isArray()) {
                    next.add(json.getNodeFactory().numberNode(n.size()));
                    continue;
                }
                JsonNode v = m.group(1).isEmpty() ? n : n.get(m.group(1));
                List<JsonNode> vs = v == null ? List.of() : List.of(v);
                Matcher idx = Pattern.compile("\\[(\\d+|\\*)]").matcher(m.group(2));
                while (idx.find()) {
                    List<JsonNode> expanded = new ArrayList<>();
                    for (JsonNode x : vs) {
                        if ("*".equals(idx.group(1))) {
                            x.forEach(expanded::add);
                            collect = true;
                        } else if (x.get(Integer.parseInt(idx.group(1))) != null) {
                            expanded.add(x.get(Integer.parseInt(idx.group(1))));
                        }
                    }
                    vs = expanded;
                }
                next.addAll(vs);
            }
            current = next;
        }
        if (collect) {
            var array = json.createArrayNode();
            current.forEach(array::add);
            return array;
        }
        return current.isEmpty() ? null : current.get(0);
    }

    @SuppressWarnings("unchecked")
    private Object substAll(Object value, Map<String, String> vars) {
        if (value instanceof String s) {
            return subst(s, vars);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), substAll(v, vars)));
            return out;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(v -> substAll(v, vars)).toList();
        }
        return value;
    }

    private static String subst(String text, Map<String, String> vars) {
        Matcher m = VAR.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String v = vars.get(m.group(1));
            if (v == null) {
                throw new IllegalStateException("未定義の変数: " + m.group(1));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @AfterAll
    static void writeReport() throws IOException {
        List<String> lines = new ArrayList<>(List.of(
                "# test/scenarios 実行結果", "",
                "| ID | シナリオ | ファイル | 結果 |", "|---|---|---|---|"));
        lines.addAll(REPORT.stream().sorted().toList());
        Files.write(Path.of("target", "scenario-report.md"), lines, StandardCharsets.UTF_8);
    }
}
