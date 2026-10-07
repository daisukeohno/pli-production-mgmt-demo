package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class SystemDateTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    private static SystemDate at(String instant) {
        return new SystemDate(Clock.fixed(Instant.parse(instant), TOKYO));
    }

    @Test
    void yearBoundaryUsesTokyoCalendarYear() {
        // 2026-12-31 23:59:59.999999 JST
        SystemDate before = at("2026-12-31T14:59:59.999999Z");
        assertThat(before.today()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(before.seqYear()).isEqualTo("26");

        // 2027-01-01 00:00:00 JST（UTC ではまだ 2026-12-31）
        SystemDate after = at("2026-12-31T15:00:00Z");
        assertThat(after.today()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(after.todayText()).isEqualTo("2027-01-01");
        assertThat(after.seqYear()).isEqualTo("27");
    }

    @Test
    void seqYearIsCalendarYearNotFiscalYear() {
        // カタログ D-04：4 月始まりの年度ではなく暦年。2027-03-31 は "27"
        assertThat(at("2027-03-31T03:00:00Z").seqYear()).isEqualTo("27");
        assertThat(at("2099-06-01T03:00:00Z").seqYear()).isEqualTo("99");
        assertThat(at("2100-06-01T03:00:00Z").seqYear()).isEqualTo("00");
    }

    @Test
    void todayTextIsHyphenSeparated() {
        assertThat(at("2026-10-07T00:00:00Z").todayText()).isEqualTo("2026-10-07");
    }

    @Test
    void nowIsTruncatedToMicroseconds() {
        SystemDate sd = at("2026-10-07T00:00:00.123456789Z");
        assertThat(sd.now()).isEqualTo(LocalDateTime.of(2026, 10, 7, 9, 0, 0, 123_456_000));
    }
}
