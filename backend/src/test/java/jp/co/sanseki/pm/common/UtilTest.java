package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jp.co.sanseki.pm.common.util.Db2Timestamp;
import jp.co.sanseki.pm.common.util.FixedChar;
import jp.co.sanseki.pm.common.util.SystemClock;
import org.junit.jupiter.api.Test;

class UtilTest {

    @Test
    void db2TimestampRoundTrip() {
        LocalDateTime t = Db2Timestamp.parse("2026-09-01-09.00.00.000000");
        assertThat(t).isEqualTo(LocalDateTime.of(2026, 9, 1, 9, 0));
        assertThat(Db2Timestamp.format(LocalDateTime.of(2026, 10, 16, 7, 8, 9, 123_456_789)))
                .isEqualTo("2026-10-16-07.08.09.123456");
        assertThat(Db2Timestamp.tryParse("2026-09-01 09:00:00")).isEmpty();
        assertThat(Db2Timestamp.tryParse("2026-02-30-09.00.00.000000")).isEmpty();
        assertThat(Db2Timestamp.tryParse(null)).isEmpty();
    }

    @Test
    void fixedCharBehavesLikeDb2Char() {
        assertThat(FixedChar.rtrim("個   ")).isEqualTo("個");
        assertThat(FixedChar.rtrim("  A  ")).isEqualTo("  A");
        assertThat(FixedChar.isBlank("    ")).isTrue();
        assertThat(FixedChar.isBlank(null)).isTrue();
        assertThat(FixedChar.fits("精密減速機ユニットA型", 20)).isTrue();
        assertThat(FixedChar.fits("12345678901234567890", 20)).isTrue();
        assertThat(FixedChar.fits("123456789012345678901", 20)).isFalse();
        assertThat(FixedChar.fits("12345678901234567890   ", 20)).isTrue();
        assertThat(FixedChar.pad("個", 4)).isEqualTo("個   ");
    }

    @Test
    void getSysDate() {
        Clock fixed = Clock.fixed(Instant.parse("2026-12-31T15:30:00Z"), ZoneId.of("Asia/Tokyo"));
        SystemClock clock = new SystemClock(fixed);
        assertThat(clock.sysDate()).isEqualTo("2027-01-01");
        assertThat(SystemClock.SCREEN_DATE.format(clock.today())).isEqualTo("2027/01/01");
    }
}
