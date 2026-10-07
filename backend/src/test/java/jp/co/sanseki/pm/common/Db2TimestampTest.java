package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class Db2TimestampTest {

    @Test
    void roundTrip() {
        LocalDateTime t = LocalDateTime.of(2026, 9, 1, 9, 0, 0, 123_456_000);
        assertThat(Db2Timestamp.format(t)).isEqualTo("2026-09-01-09.00.00.123456");
        assertThat(Db2Timestamp.parse("2026-09-01-09.00.00.123456")).isEqualTo(t);
        assertThat(Db2Timestamp.format(Db2Timestamp.parse("2026-09-01-09.00.00.000000")))
                .isEqualTo("2026-09-01-09.00.00.000000");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"2026-09-01 09:00:00", "2026-09-01-09.00.00", "2026-02-30-09.00.00.000000",
            "2026-09-01-24.00.00.000000", "2026-09-01T09:00:00.000000"})
    void tryParseRejectsInvalid(String text) {
        assertThat(Db2Timestamp.tryParse(text)).isEmpty();
    }
}
