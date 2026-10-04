package jp.sanseki.pm.pm02;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jayway.jsonpath.JsonPath;

import jp.sanseki.pm.common.db.Db2SchemaInitializer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Pm02ConcurrencyTest {

    @LocalServerPort
    int port;

    @Autowired
    Db2SchemaInitializer db;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        db.reset();
    }

    @Test
    void 別端末から同時に出庫しても在庫を超えて出庫されない() throws Exception {
        // 20000103（部品）在庫 25 に対し 6 端末が 5 ずつ出庫 → 5 件 M012、1 件 M011、在庫 0
        List<HttpResponse<String>> res = concurrently(6,
                "{\"aid\":\"ENTER\",\"itemCd\":\"20000103\",\"ioKbn\":\"2\",\"ioQty\":\"5\"}");
        List<String> ids = new ArrayList<>();
        for (HttpResponse<String> r : res) {
            assertThat(r.statusCode()).isIn(200, 422);
            ids.add(JsonPath.read(r.body(), "$.msgId"));
        }
        assertThat(ids.stream().filter("M012"::equals).count()).isEqualTo(5);
        assertThat(ids.stream().filter("M011"::equals).count()).isEqualTo(1);
        assertThat(stockQty("20000103")).isEqualByComparingTo("0");
    }

    @Test
    void 別端末から同時に入庫しても加算が失われない() throws Exception {
        List<HttpResponse<String>> res = concurrently(8,
                "{\"aid\":\"ENTER\",\"itemCd\":\"20000028\",\"ioKbn\":\"1\",\"ioQty\":\"3\"}");
        for (HttpResponse<String> r : res) {
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat((String) JsonPath.read(r.body(), "$.msgId")).isEqualTo("M012");
        }
        assertThat(stockQty("20000028")).isEqualByComparingTo("104");
    }

    @Test
    void STOCK行の無い品目へ同時に入庫しても500にならず全件加算される() throws Exception {
        // PM01 で登録直後（STOCK 行なし）の品目を想定
        jdbc.update("INSERT INTO SANSEKI.ITEM_MST (ITEM_CD, ITEM_NAME, ITEM_KBN, STOCK_UNIT, DEL_FLG, UPD_TMS, CRT_TMS)"
                + " VALUES ('30000018', '新規部品', '2', '個', '0', CURRENT TIMESTAMP, CURRENT TIMESTAMP)");
        HttpClient a = client();
        send(a, "GET", null);
        HttpResponse<String> inq = send(a, "POST", "{\"aid\":\"ENTER\",\"itemCd\":\"30000018\"}");
        assertThat(inq.statusCode()).isEqualTo(200);
        assertThat((Integer) JsonPath.read(inq.body(), "$.fields.stockQty")).isZero();

        List<HttpResponse<String>> res = concurrently(6,
                "{\"aid\":\"ENTER\",\"itemCd\":\"30000018\",\"ioKbn\":\"1\",\"ioQty\":\"7\"}");
        for (HttpResponse<String> r : res) {
            assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
            assertThat((String) JsonPath.read(r.body(), "$.msgId")).isEqualTo("M012");
        }
        assertThat(stockQty("30000018")).isEqualByComparingTo("42");
    }

    @Test
    void STOCK行の無い部品の出庫はM011で行を作らない() throws Exception {
        jdbc.update("INSERT INTO SANSEKI.ITEM_MST (ITEM_CD, ITEM_NAME, ITEM_KBN, STOCK_UNIT, DEL_FLG, UPD_TMS, CRT_TMS)"
                + " VALUES ('30000018', '新規部品', '2', '個', '0', CURRENT TIMESTAMP, CURRENT TIMESTAMP)");
        HttpClient a = client();
        send(a, "GET", null);
        HttpResponse<String> r = send(a, "POST", "{\"aid\":\"ENTER\",\"itemCd\":\"30000018\",\"ioKbn\":\"2\",\"ioQty\":\"1\"}");
        assertThat(r.statusCode()).isEqualTo(422);
        assertThat((String) JsonPath.read(r.body(), "$.msgId")).isEqualTo("M011");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SANSEKI.STOCK WHERE ITEM_CD = '30000018'", Integer.class))
                .isZero();
    }

    @Test
    void 製品区分もマイナス在庫を許容せずM011() throws Exception {
        HttpClient a = client();
        send(a, "GET", null);
        HttpResponse<String> r = send(a, "POST", "{\"aid\":\"ENTER\",\"itemCd\":\"10000021\",\"ioKbn\":\"2\",\"ioQty\":\"11\"}");
        assertThat(r.statusCode()).isEqualTo(422);
        assertThat((String) JsonPath.read(r.body(), "$.msgId")).isEqualTo("M011");
        assertThat(stockQty("10000021")).isEqualByComparingTo("10");
    }

    @Test
    void 数量の境界と桁あふれ() throws Exception {
        HttpClient a = client();
        send(a, "GET", null);
        assertMsg(send(a, "POST", "{\"aid\":\"ENTER\",\"itemCd\":\"20000028\",\"ioKbn\":\"1\",\"ioQty\":\"\"}"), 400, "M004", "F-IOQTY");
        assertMsg(send(a, "POST", "{\"aid\":\"ENTER\",\"itemCd\":\"20000028\",\"ioKbn\":\"\",\"ioQty\":\"5\"}"), 400, "M010", "F-IOKBN");
        assertMsg(send(a, "POST", "{\"aid\":\"ENTER\",\"itemCd\":\"20000028\",\"ioKbn\":\"1\",\"ioQty\":\"-5\"}"), 400, "M008", "F-IOQTY");
        assertMsg(send(a, "POST", "{\"aid\":\"ENTER\",\"itemCd\":\"20000028\",\"ioKbn\":\"1\",\"ioQty\":\"0\"}"), 400, "M009", "F-IOQTY");
        assertMsg(send(a, "POST", "{\"aid\":\"ENTER\",\"itemCd\":\"20000028\",\"ioKbn\":\"1\",\"ioQty\":\"1000000000\"}"), 400, "M009", "F-IOQTY");
        // 80 + 999999999 は DEC(9,0) を超える
        assertMsg(send(a, "POST", "{\"aid\":\"ENTER\",\"itemCd\":\"20000028\",\"ioKbn\":\"1\",\"ioQty\":\"999999999\"}"), 400, "M009", "F-IOQTY");
        assertMsg(send(a, "POST", "{\"aid\":\"ENTER\",\"itemCd\":\"200000280\"}"), 400, "M004", "F-ITEMCD");
        assertThat(stockQty("20000028")).isEqualByComparingTo("80");
    }

    @Test
    void 論理削除済の品目はM002() throws Exception {
        jdbc.update("UPDATE SANSEKI.ITEM_MST SET DEL_FLG = '1' WHERE ITEM_CD = '20000028'");
        HttpClient a = client();
        send(a, "GET", null);
        assertMsg(send(a, "POST", "{\"aid\":\"ENTER\",\"itemCd\":\"20000028\",\"ioKbn\":\"1\",\"ioQty\":\"1\"}"), 404, "M002", "F-ITEMCD");
        assertThat(stockQty("20000028")).isEqualByComparingTo("80");
    }

    @Test
    void PF3で終了しPF9は400() throws Exception {
        HttpClient a = client();
        send(a, "GET", null);
        assertThat(send(a, "POST", "{\"aid\":\"PF9\"}").statusCode()).isEqualTo(400);
        HttpResponse<String> r = send(a, "POST", "{\"aid\":\"PF3\"}");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(r.body(), "$.ended")).isTrue();
    }

    private void assertMsg(HttpResponse<String> r, int status, String msgId, String fieldPos) {
        assertThat(r.statusCode()).as(r.body()).isEqualTo(status);
        assertThat((String) JsonPath.read(r.body(), "$.msgId")).isEqualTo(msgId);
        assertThat((String) JsonPath.read(r.body(), "$.fieldPos")).isEqualTo(fieldPos);
    }

    private BigDecimal stockQty(String itemCd) {
        return jdbc.queryForObject("SELECT STOCK_QTY FROM SANSEKI.STOCK WHERE ITEM_CD = ?", BigDecimal.class, itemCd);
    }

    private List<HttpResponse<String>> concurrently(int n, String body) throws Exception {
        List<HttpClient> terminals = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            HttpClient c = client();
            send(c, "GET", null);
            terminals.add(c);
        }
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            List<Future<HttpResponse<String>>> futures = new ArrayList<>();
            for (HttpClient c : terminals) {
                futures.add(pool.submit((Callable<HttpResponse<String>>) () -> send(c, "POST", body)));
            }
            List<HttpResponse<String>> res = new ArrayList<>();
            for (Future<HttpResponse<String>> f : futures) {
                res.add(f.get());
            }
            return res;
        } finally {
            pool.shutdown();
        }
    }

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    private HttpResponse<String> send(HttpClient c, String method, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/pm02"))
                .header("Content-Type", "application/json");
        if ("GET".equals(method)) {
            b.GET();
        } else {
            b.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        return c.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
