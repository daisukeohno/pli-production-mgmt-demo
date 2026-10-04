package jp.sanseki.pm.pm01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jayway.jsonpath.JsonPath;

import jp.sanseki.pm.common.db.Db2SchemaInitializer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Pm01ConcurrencyTest {

    @LocalServerPort
    int port;

    @Autowired
    Db2SchemaInitializer db;

    @Autowired
    JdbcTemplate jdbc;

    @SpyBean
    ItemMstRepository repo;

    @BeforeEach
    void reset() {
        db.reset();
    }

    @Test
    void 存在確認後に他端末が登録済みなら主キー重複をM003で返す() throws Exception {
        // 端末 B が先に 20000073 を登録した直後に、端末 A の存在確認が通った状況を再現
        doReturn(false).when(repo).exists(eq("20000073"));
        HttpClient a = client();
        send(a, "GET", null);
        HttpResponse<String> res = send(a, "POST",
                "{\"aid\":\"ENTER\",\"func\":\"2\",\"itemCd\":\"20000073\",\"itemName\":\"重複\",\"itemKbn\":\"2\",\"stockUnit\":\"個\"}");
        assertThat(res.statusCode()).isEqualTo(409);
        assertThat((String) JsonPath.read(res.body(), "$.msgId")).isEqualTo("M003");
        assertThat((String) JsonPath.read(res.body(), "$.fieldPos")).isEqualTo("F-ITEMCD");
    }

    @Test
    void 別端末から同一コードを同時登録しても500にならず1件だけ登録される() throws Exception {
        int n = 6;
        List<HttpClient> terminals = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            HttpClient c = client();
            send(c, "GET", null);
            terminals.add(c);
        }
        String body = "{\"aid\":\"ENTER\",\"func\":\"2\",\"itemCd\":\"30000018\",\"itemName\":\"同時登録\",\"itemKbn\":\"2\",\"stockUnit\":\"個\"}";
        ExecutorService pool = Executors.newFixedThreadPool(n);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        for (HttpClient c : terminals) {
            futures.add(pool.submit((Callable<HttpResponse<String>>) () -> send(c, "POST", body)));
        }
        List<String> ids = new ArrayList<>();
        for (Future<HttpResponse<String>> f : futures) {
            HttpResponse<String> r = f.get();
            assertThat(r.statusCode()).isIn(200, 409);
            ids.add(JsonPath.read(r.body(), "$.msgId"));
        }
        pool.shutdown();
        assertThat(ids).containsOnly("M005", "M003");
        assertThat(ids.stream().filter("M005"::equals).count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SANSEKI.ITEM_MST WHERE ITEM_CD = '30000018'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void 同一端末からの並行リクエストは直列化されCOMMAREAが壊れない() throws Exception {
        HttpClient a = client();
        send(a, "GET", null);
        int n = 20;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String aid = i % 2 == 0 ? "PF8" : "PF7";
            futures.add(pool.submit((Callable<HttpResponse<String>>) () -> send(a, "POST", "{\"aid\":\"" + aid + "\"}")));
        }
        for (Future<HttpResponse<String>> f : futures) {
            HttpResponse<String> r = f.get();
            assertThat(r.statusCode()).isEqualTo(200);
            int page = JsonPath.read(r.body(), "$.pageNo");
            List<String> codes = JsonPath.read(r.body(), "$.list[*].itemCd");
            // 頁番号と一覧の内容が一致していること（1 頁目=10 件、2 頁目=5 件）
            assertThat(codes).hasSize(page == 1 ? 10 : 5);
        }
        pool.shutdown();
    }

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    private HttpResponse<String> send(HttpClient c, String method, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/pm01"))
                .header("Content-Type", "application/json");
        if ("GET".equals(method)) {
            b.GET();
        } else {
            b.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        return c.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
