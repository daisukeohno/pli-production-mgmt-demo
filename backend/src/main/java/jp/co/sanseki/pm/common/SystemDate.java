package jp.co.sanseki.pm.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

/**
 * システム日付・時刻（PMUTL01 GET-SYS-DATE の移植）。現在時刻は必ずここから取る。
 * Clock を注入するので、テストでは任意の日時（年またぎ等）に固定できる。
 */
@Component
public class SystemDate {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd");
    private static final DateTimeFormatter YY = DateTimeFormatter.ofPattern("uu");

    private final Clock clock;

    public SystemDate(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** GET-SYS-DATE の返却値と同じ YYYY-MM-DD（ハイフン区切り）。 */
    public String todayText() {
        return today().format(ISO_DATE);
    }

    /** 採番の年：システム日付の西暦（暦年）下 2 桁（カタログ D-04 / D-05）。 */
    public String seqYear() {
        return today().format(YY);
    }

    /** UPD_TMS / CRT_TMS に入れる現在時刻。DB2 TIMESTAMP と同じマイクロ秒精度に切り捨てる。 */
    public LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
