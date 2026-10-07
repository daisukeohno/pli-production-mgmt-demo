package jp.co.sanseki.pm.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** /actuator/health の "sanseki"：SANSEKI スキーマ 5 テーブルの件数を返す（読めなければ DOWN）。 */
@Component("sanseki")
public class SansekiTablesHealthIndicator extends AbstractHealthIndicator {

    static final List<String> TABLES = List.of("ITEM_MST", "BOM", "STOCK", "WORK_ORDER", "SEQ_CTL");

    private final JdbcClient jdbc;

    public SansekiTablesHealthIndicator(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : TABLES) {
            counts.put(table, jdbc.sql("SELECT COUNT(*) FROM SANSEKI." + table).query(Long.class).single());
        }
        builder.up().withDetail("schema", "SANSEKI").withDetail("rowCounts", counts);
    }
}
