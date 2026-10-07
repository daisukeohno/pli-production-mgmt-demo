package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OptimisticLockTest {

    private static final LocalDateTime CURRENT = LocalDateTime.of(2026, 9, 1, 9, 0, 0, 1_000);

    @Test
    void sameTimestampPasses() {
        assertThatCode(() -> OptimisticLock.verify("2026-09-01-09.00.00.000001", CURRENT)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"2026-09-01-09.00.00.000000", "2026-09-01-09.00.00.000002", "garbage"})
    void mismatchOrMissingIsM017(String requested) {
        assertThatThrownBy(() -> OptimisticLock.verify(requested, CURRENT))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    org.assertj.core.api.Assertions.assertThat(e.msg()).isEqualTo(MessageCatalog.M017);
                });
    }
}
