package jp.sanseki.pm.pm03;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jayway.jsonpath.JsonPath;

import jp.sanseki.pm.common.db.Db2SchemaInitializer;
import jp.sanseki.pm.common.util.Pmutl01;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Pm03ConcurrencyTest {

    @LocalServerPort
    int port;

    @Autowired
    Db2SchemaInitializer db;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Pmutl01 pmutl01;

    @BeforeEach
    void reset() {
        db.reset();
    }

    @Test
    void 別端末からの同時登録でも製造指示番号が重複しない() throws Exception {
        // 年が替わると連番は 1 へ振り直されるため、実行時点の年度で SEQ_NO=2 に揃えておく
        jdbc.update("UPDATE SANSEKI.SEQ_CTL SET SEQ_YY = ?, SEQ_NO = 2 WHERE SEQ_NAME = 'WORK_ORDER'",
                currentYy());
        int n = 6;
        List<HttpClient> terminals = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            HttpClient c = client();
            send(c, "GET", null);
            terminals.add(c);
        }
        String body = "{\"aid\":\"ENTER\",\"itemCd\":\"10000014\",\"orderQty\":\"1\",\"dueDate\":\"2026-12-01\"}";
        ExecutorService pool = Executors.newFixedThreadPool(n);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        for (HttpClient c : terminals) {
            futures.add(pool.submit((Callable<HttpResponse<String>>) () -> send(c, "POST", body)));
        }
        Set<String> orderNos = new HashSet<>();
        for (Future<HttpResponse<String>> f : futures) {
            HttpResponse<String> r = f.get();
            assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
            assertThat((String) JsonPath.read(r.body(), "$.msgId")).isEqualTo("M013");
            orderNos.add(JsonPath.read(r.body(), "$.fields.orderNo"));
        }
        pool.shutdown();
        // SEQ_NO=2 からの同時登録 n 件で +n まで一意に採番される（年度は実行時点の値）
        assertThat(orderNos).hasSize(n);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SANSEKI.WORK_ORDER", Integer.class))
                .isEqualTo(2 + n);
        assertThat(jdbc.queryForObject(
                "SELECT SEQ_NO FROM SANSEKI.SEQ_CTL WHERE SEQ_NAME = 'WORK_ORDER'", Integer.class))
                .isEqualTo(2 + n);
    }

    @Test
    void 同一端末からの並行登録は直列化され個別に採番される() throws Exception {
        HttpClient a = client();
        send(a, "GET", null);
        int n = 6;
        String body = "{\"aid\":\"ENTER\",\"itemCd\":\"10000014\",\"orderQty\":\"1\",\"dueDate\":\"2026-12-01\"}";
        ExecutorService pool = Executors.newFixedThreadPool(n);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit((Callable<HttpResponse<String>>) () -> send(a, "POST", body)));
        }
        Set<String> orderNos = new HashSet<>();
        for (Future<HttpResponse<String>> f : futures) {
            HttpResponse<String> r = f.get();
            assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
            assertThat((String) JsonPath.read(r.body(), "$.msgId")).isEqualTo("M013");
            orderNos.add(JsonPath.read(r.body(), "$.fields.orderNo"));
        }
        pool.shutdown();
        assertThat(orderNos).hasSize(n);
    }

    @Test
    void 在庫不足で同時に失敗しても採番を消費しない() throws Exception {
        // 10000021 を 50 台指示すると 20000042 が不足（シナリオ PM03-02 と同条件）
        int n = 4;
        List<HttpClient> terminals = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            HttpClient c = client();
            send(c, "GET", null);
            terminals.add(c);
        }
        String body = "{\"aid\":\"ENTER\",\"itemCd\":\"10000021\",\"orderQty\":\"50\",\"dueDate\":\"2026-12-01\"}";
        ExecutorService pool = Executors.newFixedThreadPool(n);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        for (HttpClient c : terminals) {
            futures.add(pool.submit((Callable<HttpResponse<String>>) () -> send(c, "POST", body)));
        }
        for (Future<HttpResponse<String>> f : futures) {
            HttpResponse<String> r = f.get();
            assertThat(r.statusCode()).as(r.body()).isEqualTo(422);
            assertThat((String) JsonPath.read(r.body(), "$.msgId")).isEqualTo("M015");
        }
        pool.shutdown();
        // 全件 M015 のため WORK_ORDER も SEQ_NO も変化しない
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SANSEKI.WORK_ORDER", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT SEQ_NO FROM SANSEKI.SEQ_CTL WHERE SEQ_NAME = 'WORK_ORDER'", Integer.class))
                .isEqualTo(2);
    }

    private String currentYy() {
        return pmutl01.getSysDate().substring(2, 4);
    }

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    private HttpResponse<String> send(HttpClient c, String method, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/pm03"))
                .header("Content-Type", "application/json");
        if ("GET".equals(method)) {
            b.GET();
        } else {
            b.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        return c.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
