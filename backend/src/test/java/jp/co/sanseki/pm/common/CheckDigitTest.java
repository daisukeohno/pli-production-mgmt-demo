package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;

import jp.co.sanseki.pm.common.util.CheckDigit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;

/** PMUTL01 CALC_CHK_DGT（モジュラス10・ウェイト3-1）と同じ結果になることを検証する。 */
class CheckDigitTest {

    /** PL/I の計算式をそのまま手計算した期待値。 */
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            // 1*3 + 0 + 0 + 0 + 0 + 0 + 1*3 = 6 -> (10-6)%10 = 4
            "1000001, 4",
            // 2*3 + 1*3 = 9 -> 1
            "2000001, 1",
            // 2*3 + 1*1 + 1*3 = 10 -> MOD 10 = 0 -> (10-0)%10 = 0
            "2000011, 0",
            // 9*3 + 1*3 = 30 -> 0
            "9000001, 0",
            // 1*3+2*1+3*3+4*1+5*3+6*1+7*3 = 60 -> 0
            "1234567, 0",
            // 9*3+9+9*3+9+9*3+9+9*3 = 135 -> 5 -> 5
            "9999999, 5",
            "0000000, 0",
            // 3 -> 7
            "0000001, 7"
    })
    void calculate_matchesPmutl01(String cd7, char expected) {
        assertThat(CheckDigit.calculate(cd7)).isEqualTo(expected);
    }

    @Test
    void initialData_allItemCodesAreValid() throws Exception {
        String csv = new ClassPathResource("legacy/db2/data/item_mst.csv").getContentAsString(StandardCharsets.UTF_8);
        List<String> codes = csv.lines().skip(1).filter(l -> !l.isBlank()).map(l -> l.split(",")[0]).toList();
        assertThat(codes).hasSize(15).allSatisfy(cd -> assertThat(CheckDigit.isValid(cd)).as(cd).isTrue());
    }

    @ParameterizedTest
    @CsvSource({"20000110, true", "20000111, false", "10000014, true", "10000015, false"})
    void isValid(String cd8, boolean expected) {
        assertThat(CheckDigit.isValid(cd8)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL", "''", "1000001", "100000144", "1000001A", "A0000014", "1000 014"}, nullValues = "NULL")
    void isValid_rejectsMalformed(String cd8) {
        assertThat(CheckDigit.isValid(cd8)).isFalse();
    }

    @Test
    void calculate_rejectsNonDigits() {
        assertThatThrownBy(() -> CheckDigit.calculate("12A4567")).isInstanceOf(IllegalArgumentException.class);
    }

    /** 2003-02-10 廃止の旧モジュラス11方式（ウェイト 7..1）は移植しない：結果が異なるコードで確認。 */
    @Test
    void legacyModulus11IsNotUsed() {
        String cd7 = "2000001";
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += (cd7.charAt(i) - '0') * (7 - i);
        }
        char mod11 = (char) ('0' + (11 - sum % 11) % 11);
        // 2*7 + 1*1 = 15 -> (11 - 15 MOD 11) MOD 11 = 7
        assertThat(mod11).isEqualTo('7');
        assertThat(CheckDigit.calculate(cd7)).isEqualTo('1').isNotEqualTo(mod11);
        assertThat(CheckDigit.isValid("20000017")).isFalse();
    }
}
