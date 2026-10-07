package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CharsTest {

    @Test
    void trimsOnlyTrailingHalfWidthSpaces() {
        assertThat(Chars.trimPadding("ABC     ")).isEqualTo("ABC");
        assertThat(Chars.trimPadding("  ABC  ")).isEqualTo("  ABC");
        assertThat(Chars.trimPadding("台\u3000  ")).isEqualTo("台\u3000");
        assertThat(Chars.trimPadding("    ")).isEmpty();
        assertThat(Chars.trimPadding(null)).isNull();
    }
}
