package jp.co.sanseki.pm.common.api;

import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * シナリオテスト用 API（scenario プロファイル＝pm.test-support.enabled=true のときのみ有効）。
 * DB を初期データ（db2/data）の状態へ戻し、DB の生の行を参照する。
 */
@RestController
@RequestMapping("/api/test-support")
@ConditionalOnProperty(name = "pm.test-support.enabled", havingValue = "true")
public class TestSupportController {

    private final Flyway flyway;
    private final JdbcClient jdbc;

    public TestSupportController(Flyway flyway, JdbcClient jdbc) {
        this.flyway = flyway;
        this.jdbc = jdbc;
    }

    @PostMapping("/reset")
    public Map<String, String> reset() {
        flyway.clean();
        flyway.migrate();
        return Map.of("status", "reset");
    }

    /** ITEM_MST の生の行（論理削除行を含む、CHAR は空白埋めのまま）。 */
    @GetMapping("/item-mst/{itemCd}")
    public List<Map<String, Object>> itemMst(@PathVariable String itemCd) {
        return jdbc.sql("""
                SELECT ITEM_CD, ITEM_NAME, ITEM_KBN, STOCK_UNIT, DEL_FLG,
                       TO_CHAR(UPD_TMS, 'YYYY-MM-DD-HH24.MI.SS.US') AS UPD_TMS,
                       TO_CHAR(CRT_TMS, 'YYYY-MM-DD-HH24.MI.SS.US') AS CRT_TMS
                  FROM ITEM_MST WHERE ITEM_CD = :cd""")
                .param("cd", itemCd)
                .query()
                .listOfRows();
    }
}
