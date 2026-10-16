package jp.co.sanseki.pm.scenario;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * test/scenarios/{screen}/*.json を実 HTTP で実行するランナー。
 * <p>形式は test/README.md 参照。各シナリオの前に /api/test-support/reset で DB を初期データへ戻す。</p>
 */
public final class ScenarioRunner {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern VAR = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    private final String baseUrl;
    private final HttpClient http = HttpClient.newHttpClient();

    public ScenarioRunner(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public record Scenario(String file, JsonNode json) {

        public String id() {
            return json.path("id").asText();
        }

        public String title() {
            return json.path("title").asText();
        }
    }

    public record StepResult(String name, boolean passed, String detail) {
    }

    public record ScenarioResult(Scenario scenario, List<StepResult> steps) {

        public boolean passed() {
            return steps.stream().allMatch(StepResult::passed);
        }
    }

    public static List<Scenario> load(String screen) throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:scenarios/" + screen + "/*.json");
        List<Scenario> list = new ArrayList<>();
        Arrays.stream(resources).sorted((a, b) -> a.getFilename().compareTo(b.getFilename())).forEach(r -> {
            try (InputStream in = r.getInputStream()) {
                list.add(new Scenario(r.getFilename(), JSON.readTree(in)));
            } catch (IOException e) {
                throw new IllegalStateException(r.getFilename(), e);
            }
        });
        return list;
    }

    public ScenarioResult run(Scenario scenario) {
        List<StepResult> results = new ArrayList<>();
        Map<String, String> vars = new HashMap<>();
        try {
            send("POST", "/api/test-support/reset", null, 200);
        } catch (Exception e) {
            results.add(new StepResult("DB初期化（/api/test-support/reset）", false, e.toString()));
            return new ScenarioResult(scenario, results);
        }
        for (JsonNode step : scenario.json().path("steps")) {
            String name = step.path("name").asText();
            List<String> errors = new ArrayList<>();
            try {
                JsonNode req = step.path("request");
                String path = substitute(req.path("path").asText(), vars);
                JsonNode body = req.has("body") ? substitute(req.get("body"), vars) : null;
                HttpResponse<String> res = send(req.path("method").asText("GET"), path, body, -1);
                JsonNode actual = res.body().isBlank() ? JSON.nullNode() : JSON.readTree(res.body());

                JsonNode expect = step.path("expect");
                if (expect.has("status") && expect.get("status").asInt() != res.statusCode()) {
                    errors.add("status: expected " + expect.get("status").asInt() + " but " + res.statusCode() + " body=" + res.body());
                }
                if (expect.has("body")) {
                    match("$", substitute(expect.get("body"), vars), actual, errors);
                }
                for (JsonNode a : expect.path("assert")) {
                    check(a, actual, vars, errors);
                }
                step.path("capture").fields().forEachRemaining(e -> {
                    List<JsonNode> found = select(actual, e.getValue().asText());
                    if (found.isEmpty() || found.get(0).isNull()) {
                        errors.add("capture " + e.getKey() + ": " + e.getValue().asText() + " が見つかりません");
                    } else {
                        vars.put(e.getKey(), found.get(0).asText());
                    }
                });
                String detail = req.path("method").asText("GET") + " " + path + " -> " + res.statusCode()
                        + (actual.has("msgId") && !actual.get("msgId").isNull() ? " " + actual.get("msgId").asText() : "");
                results.add(new StepResult(name, errors.isEmpty(), errors.isEmpty() ? detail : detail + "\n  " + String.join("\n  ", errors)));
            } catch (Exception e) {
                results.add(new StepResult(name, false, e.toString()));
            }
            if (!results.get(results.size() - 1).passed()) {
                break;
            }
        }
        return new ScenarioResult(scenario, results);
    }

    private HttpResponse<String> send(String method, String path, JsonNode body, int expectStatus) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path)).header("Accept", "application/json");
        if (body != null) {
            b.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body), StandardCharsets.UTF_8));
        } else {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (expectStatus > 0 && res.statusCode() != expectStatus) {
            throw new IllegalStateException(method + " " + path + " -> " + res.statusCode() + " " + res.body());
        }
        return res;
    }

    /** 期待値の部分一致。オブジェクトは期待側のキーのみ比較、配列は件数と要素順まで比較。"~re:" 接頭辞は正規表現。 */
    static void match(String at, JsonNode expected, JsonNode actual, List<String> errors) {
        if (expected.isObject()) {
            if (!actual.isObject()) {
                errors.add(at + ": expected object but " + actual);
                return;
            }
            expected.fields().forEachRemaining(e -> match(at + "." + e.getKey(), e.getValue(),
                    actual.has(e.getKey()) ? actual.get(e.getKey()) : JSON.missingNode(), errors));
        } else if (expected.isArray()) {
            if (!actual.isArray() || actual.size() != expected.size()) {
                errors.add(at + ": expected " + expected.size() + " items but " + actual);
                return;
            }
            for (int i = 0; i < expected.size(); i++) {
                match(at + "[" + i + "]", expected.get(i), actual.get(i), errors);
            }
        } else if (expected.isTextual() && expected.asText().startsWith("~re:")) {
            if (!actual.isTextual() || !actual.asText().matches(expected.asText().substring(4))) {
                errors.add(at + ": expected /" + expected.asText().substring(4) + "/ but " + actual);
            }
        } else if (!expected.equals(actual)) {
            errors.add(at + ": expected " + expected + " but " + actual);
        }
    }

    private static void check(JsonNode a, JsonNode actual, Map<String, String> vars, List<String> errors) {
        String path = a.path("path").asText();
        List<JsonNode> values = select(actual, path);
        if (a.has("equals")) {
            JsonNode expected = substitute(a.get("equals"), vars);
            JsonNode got = path.contains("[*]") ? JSON.valueToTree(values) : (values.isEmpty() ? JSON.missingNode() : values.get(0));
            match(path, expected, got, errors);
        }
        if (a.has("notEquals")) {
            JsonNode notExpected = substitute(a.get("notEquals"), vars);
            if (!values.isEmpty() && values.get(0).equals(notExpected)) {
                errors.add(path + ": must not be " + notExpected);
            }
        }
        if (a.has("contains") && values.stream().noneMatch(v -> v.equals(a.get("contains")))) {
            errors.add(path + ": must contain " + a.get("contains") + " but " + values);
        }
        if (a.has("notContains") && values.stream().anyMatch(v -> v.equals(a.get("notContains")))) {
            errors.add(path + ": must not contain " + a.get("notContains") + " but " + values);
        }
        if (a.has("size") && values.size() != a.get("size").asInt()) {
            errors.add(path + ": expected size " + a.get("size").asInt() + " but " + values.size());
        }
    }

    /** "rows[*].itemCd" / "rows[0].itemName" / "msgId" 形式のパス。 */
    static List<JsonNode> select(JsonNode root, String path) {
        List<JsonNode> current = new ArrayList<>(List.of(root));
        if (path.isEmpty() || path.equals("$")) {
            return current;
        }
        for (String token : path.split("\\.")) {
            Matcher m = Pattern.compile("^([^\\[]*)(?:\\[(\\*|\\d+)])?$").matcher(token);
            if (!m.matches()) {
                throw new IllegalArgumentException("不正なパス: " + path);
            }
            List<JsonNode> next = new ArrayList<>();
            for (JsonNode node : current) {
                JsonNode child = m.group(1).isEmpty() ? node : node.path(m.group(1));
                if (m.group(2) == null) {
                    if (!child.isMissingNode()) {
                        next.add(child);
                    }
                } else if (m.group(2).equals("*")) {
                    child.forEach(next::add);
                } else if (child.has(Integer.parseInt(m.group(2)))) {
                    next.add(child.get(Integer.parseInt(m.group(2))));
                }
            }
            current = next;
        }
        return current;
    }

    static String substitute(String text, Map<String, String> vars) {
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

    static JsonNode substitute(JsonNode node, Map<String, String> vars) {
        if (node.isTextual()) {
            return new TextNode(substitute(node.asText(), vars));
        }
        if (node.isObject()) {
            ObjectNode copy = JSON.createObjectNode();
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                copy.set(e.getKey(), substitute(e.getValue(), vars));
            }
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = JSON.createArrayNode();
            node.forEach(n -> copy.add(substitute(n, vars)));
            return copy;
        }
        return node;
    }

    /** 実行結果の Markdown レポート（PR 掲載用）。 */
    public static String report(String screen, List<ScenarioResult> results) {
        StringBuilder sb = new StringBuilder();
        long ok = results.stream().filter(ScenarioResult::passed).count();
        sb.append("# ").append(screen.toUpperCase()).append(" シナリオ実行結果：").append(ok).append("/").append(results.size()).append(" 合格\n\n");
        sb.append("| ID | シナリオ | ステップ | 結果 |\n|---|---|---|---|\n");
        for (ScenarioResult r : results) {
            long stepsOk = r.steps().stream().filter(StepResult::passed).count();
            sb.append("| ").append(r.scenario().id()).append(" | ").append(r.scenario().title()).append(" | ")
                    .append(stepsOk).append("/").append(r.scenario().json().path("steps").size()).append(" | ")
                    .append(r.passed() ? "PASS" : "FAIL").append(" |\n");
        }
        for (ScenarioResult r : results) {
            sb.append("\n## ").append(r.scenario().id()).append(" ").append(r.scenario().title())
                    .append(" (").append(r.scenario().file()).append(")\n\n");
            for (StepResult s : r.steps()) {
                sb.append("- ").append(s.passed() ? "PASS" : "FAIL").append(" ").append(s.name()).append(" — `")
                        .append(s.detail().replace("\n", "` `")).append("`\n");
            }
        }
        return sb.toString();
    }
}
