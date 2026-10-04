package jp.sanseki.pm.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import jp.sanseki.pm.common.db.Db2SchemaInitializer;

/**
 * test/scenarios/ 配下の YAML シナリオを実 HTTP で実行する統合テストランナー。
 * シナリオごとに DB を初期データへ戻し、端末（terminal）ごとに独立した Cookie＝セッション＝COMMAREA を持つ。
 * status: pending のシナリオはスキップ（未実装画面用）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScenarioTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON = new ObjectMapper();

    @LocalServerPort
    int port;

    @Autowired
    Db2SchemaInitializer db;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestClock testClock;

    @TestFactory
    Stream<DynamicNode> scenarios() throws IOException {
        Path root = Path.of(System.getProperty("scenarios.dir", "../test/scenarios")).toAbsolutePath().normalize();
        assertThat(root).isDirectory();
        List<Path> screens;
        try (Stream<Path> s = Files.list(root)) {
            screens = s.filter(Files::isDirectory).sorted().toList();
        }
        List<DynamicNode> containers = new ArrayList<>();
        for (Path screen : screens) {
            List<DynamicNode> tests = new ArrayList<>();
            try (Stream<Path> s = Files.list(screen)) {
                for (Path file : s.filter(p -> p.toString().endsWith(".yaml")).sorted().toList()) {
                    Scenario sc = YAML.readValue(file.toFile(), Scenario.class);
                    tests.add(DynamicTest.dynamicTest(sc.id + " " + sc.title, file.toUri(), () -> run(sc)));
                }
            }
            containers.add(DynamicContainer.dynamicContainer(screen.getFileName().toString(), tests));
        }
        return containers.stream();
    }

    private void run(Scenario sc) throws Exception {
        Assumptions.assumeFalse("pending".equals(sc.status), sc.id + " は未実装画面のため保留（pending）");
        db.reset();
        testClock.clear();
        if (sc.preconditions != null) {
            for (String sql : sc.preconditions.sql) {
                jdbc.update(sql);
            }
            if (sc.preconditions.fixedClock != null) {
                testClock.fix(LocalDate.parse(sc.preconditions.fixedClock));
            }
        }
        Map<String, HttpClient> terminals = new HashMap<>();
        int no = 0;
        for (Step step : sc.steps) {
            no++;
            String label = sc.id + " step" + no + " [" + step.name + "]";
            if (step.request != null) {
                HttpClient client = terminals.computeIfAbsent(step.terminal == null ? "A" : step.terminal,
                        t -> HttpClient.newBuilder().cookieHandler(new CookieManager()).build());
                execHttp(label, client, step);
            }
            if (step.db != null) {
                execDb(label, step.db);
            }
        }
    }

    private void execHttp(String label, HttpClient client, Step step) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + step.request.path))
                .header("Content-Type", "application/json");
        if ("GET".equalsIgnoreCase(step.request.method)) {
            b.GET();
        } else {
            String body = step.request.body == null ? "{}" : JSON.writeValueAsString(step.request.body);
            b.method(step.request.method.toUpperCase(), HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        HttpResponse<String> res = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (step.expect == null) {
            return;
        }
        if (step.expect.status != null) {
            assertThat(res.statusCode()).as(label + " HTTP status\n" + res.body()).isEqualTo(step.expect.status);
        }
        Object doc = JsonPath.parse(res.body()).json();
        for (Check c : step.expect.body) {
            check(label, doc, c, res.body());
        }
    }

    private void execDb(String label, DbCheck dbc) {
        List<Map<String, Object>> rows = jdbc.queryForList(dbc.sql);
        Object doc = JsonPath.parse(JSON.valueToTree(rows).toString()).json();
        for (Check c : dbc.expect) {
            check(label + " DB", doc, c, rows.toString());
        }
    }

    private static void check(String label, Object doc, Check c, String raw) {
        String desc = label + " " + c.path + "\n" + raw;
        Object actual;
        try {
            actual = JsonPath.read(doc, c.path);
        } catch (PathNotFoundException e) {
            actual = null;
        }
        if (Boolean.TRUE.equals(c.isNull)) {
            assertThat(actual).as(desc).isNull();
        }
        if (c.equals != null) {
            assertThat(normalize(actual)).as(desc).isEqualTo(normalize(c.equals));
        }
        if (c.size != null) {
            assertThat(actual).as(desc).isInstanceOf(List.class);
            assertThat((List<?>) actual).as(desc).hasSize(c.size);
        }
        if (c.contains != null) {
            assertThat(asList(actual)).as(desc).contains(normalize(c.contains));
        }
        if (c.notContains != null) {
            assertThat(asList(actual)).as(desc).doesNotContain(normalize(c.notContains));
        }
        if (c.matches != null) {
            if (actual == null) {
                fail(desc + " is null");
            }
            assertThat(String.valueOf(actual)).as(desc).matches(c.matches);
        }
    }

    private static List<Object> asList(Object o) {
        if (o instanceof List<?> l) {
            return l.stream().map(ScenarioTest::normalize).toList();
        }
        return o == null ? List.of() : List.of(normalize(o));
    }

    private static Object normalize(Object o) {
        if (o instanceof List<?> l) {
            return l.stream().map(ScenarioTest::normalize).toList();
        }
        return o == null ? null : String.valueOf(o);
    }

    // ---- YAML モデル ----

    public static class Scenario {
        public String id;
        public String title;
        public String screen;
        public String status = "active";
        public String source;
        public String description;
        public Preconditions preconditions = new Preconditions();
        public List<Step> steps = List.of();
    }

    /** シナリオ開始前の前処理。sql は DB 初期化直後に実行し、fixedClock はそのシナリオ中のシステム日付（JST）を固定する。 */
    public static class Preconditions {
        public List<String> sql = List.of();
        public String fixedClock;
    }

    public static class Step {
        public String name;
        public String terminal;
        public Request request;
        public Expect expect;
        public DbCheck db;
    }

    public static class Request {
        public String method = "POST";
        public String path;
        public Map<String, Object> body;
    }

    public static class Expect {
        public Integer status;
        public List<Check> body = List.of();
    }

    public static class DbCheck {
        public String sql;
        public List<Check> expect = List.of();
    }

    public static class Check {
        public String path;
        public Object equals;
        public Integer size;
        public Object contains;
        public Object notContains;
        public String matches;
        public Boolean isNull;
    }

    /**
     * シナリオの時計固定用。ClockConfig が作るシステム Clock を @Primary で差し替え、
     * preconditions.fixedClock の指定時だけ固定時刻を返す。未指定時はシステム時計へ委譲する。
     */
    static class TestClock extends Clock {
        private final ZoneId zone;
        private Instant fixed;

        TestClock(ZoneId zone) {
            this.zone = zone;
        }

        void fix(LocalDate date) {
            fixed = date.atStartOfDay(zone).toInstant();
        }

        void clear() {
            fixed = null;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId z) {
            TestClock c = new TestClock(z);
            c.fixed = fixed;
            return c;
        }

        @Override
        public Instant instant() {
            return fixed != null ? fixed : Clock.system(zone).instant();
        }
    }

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        TestClock testClock(@Value("${pm.system.zone:Asia/Tokyo}") String zone) {
            return new TestClock(ZoneId.of(zone));
        }
    }
}
