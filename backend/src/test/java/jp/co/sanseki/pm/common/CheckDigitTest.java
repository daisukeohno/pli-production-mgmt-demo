package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jp.co.sanseki.pm.common.util.CheckDigit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CheckDigitTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            // 合計 = 3a+b+3c+d+3e+f+3g、CD = (10 - 合計 MOD 10) MOD 10
            "1000001, 4",   // 3+0+0+0+0+0+3 = 6  -> 4
            "2000001, 1",   // 6+3 = 9            -> 1
            "2000011, 0",   // 6+1+3 = 10         -> 0
            "9000001, 0",   // 27+3 = 30          -> 0
            "0000000, 0",
            "1234567, 0",   // 3+2+9+4+15+6+21=60 -> 0
            "7654321, 0"    // 21+6+15+4+9+2+3=60 -> 0
    })
    void calc(String body, char expected) {
        assertThat(CheckDigit.calc(body)).isEqualTo(expected);
    }

    @Test
    void allSeedItemCodesAreValid() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("../db2/data/item_mst.csv"), StandardCharsets.UTF_8);
        List<String> codes = lines.subList(1, lines.size()).stream().filter(l -> !l.isBlank()).map(l -> l.split(",")[0]).toList();
        assertThat(codes).hasSize(15).allSatisfy(cd -> assertThat(CheckDigit.isValid(cd)).as(cd).isTrue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"20000111", "20000112", "10000015", "2000011", "200001100", "2000011A", "        ", ""})
    void invalidCodes(String code) {
        assertThat(CheckDigit.isValid(code)).isFalse();
    }

    @Test
    void oldModulus11CodeIsRejected() {
        // 旧モジュラス11方式（ウェイト7..1）では 2000012 の CD は 4。現行方式（モジュラス10）では 7。
        assertThat(CheckDigit.isValid("20000124")).isFalse();
        assertThat(CheckDigit.isValid("20000127")).isTrue();
    }

    @Test
    void calcRejectsNonDigits() {
        assertThatThrownBy(() -> CheckDigit.calc("20000A1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CheckDigit.calc(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
