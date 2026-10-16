package jp.co.sanseki.pm.common.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

/**
 * システム日付・時刻（PMUTL01 GET_SYS_DATE / CICS ASKTIME 相当）。
 * {@link Clock} を差し替えることでテストから日付を固定できる。
 */
@Component
public class SystemClock {

    /** P-SYS-DATE（CHAR(10)：YYYY-MM-DD）。 */
    public static final DateTimeFormatter SYS_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd");
    /** 画面1行目の日付表示（YYYY/MM/DD）。 */
    public static final DateTimeFormatter SCREEN_DATE = DateTimeFormatter.ofPattern("uuuu/MM/dd");

    private final Clock clock;

    public SystemClock(Clock clock) {
        this.clock = clock;
    }

    /** GET_SYS_DATE：システム日付。 */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** GET_SYS_DATE の出力形式（YYYY-MM-DD）。 */
    public String sysDate() {
        return SYS_DATE.format(today());
    }

    /** DB2 CURRENT TIMESTAMP 相当（マイクロ秒精度）。 */
    public LocalDateTime currentTimestamp() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
