package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CheckDigitTest {

    /** db2/data/item_mst.csv（クラスパス db/data/）の全品目コード。 */
    static List<String> initialItemCodes() throws IOException {
        try (InputStream in = CheckDigitTest.class.getClassLoader().getResourceAsStream("db/data/item_mst.csv")) {
            assertThat(in).as("db/data/item_mst.csv").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .skip(1)
                    .filter(l -> !l.isBlank())
                    .map(l -> l.split(",")[0])
                    .toList();
        }
    }

    @Test
    void initialDataHas15ItemCodes() throws IOException {
        assertThat(initialItemCodes()).hasSize(15);
    }

    @ParameterizedTest
    @MethodSource("initialItemCodes")
    void allInitialItemCodesAreValid(String itemCd) {
        assertThat(CheckDigit.isValid(itemCd)).isTrue();
        assertThat(CheckDigit.calculate(itemCd.substring(0, 7))).isEqualTo(itemCd.charAt(7));
    }

    @ParameterizedTest
    @MethodSource("initialItemCodes")
    void otherCheckDigitsAreInvalid(String itemCd) {
        IntStream.rangeClosed(0, 9)
                .mapToObj(d -> itemCd.substring(0, 7) + d)
                .filter(code -> !code.equals(itemCd))
                .forEach(code -> assertThat(CheckDigit.isValid(code)).as(code).isFalse());
    }

    @Test
    void catalogExample() {
        // カタログ R1：1000001 → 3+0+3+0+3+0+3... 合計 6 → (10-6) MOD 10 = 4
        assertThat(CheckDigit.calculate("1000001")).isEqualTo('4');
    }

    @Test
    void checkDigitZeroBoundary() {
        // 合計が 10 の倍数 → (10 - 0) MOD 10 = 0。初期データ 90000010（合計 30）
        assertThat(CheckDigit.calculate("9000001")).isEqualTo('0');
        assertThat(CheckDigit.isValid("90000010")).isTrue();
        // 合計 0（全桁 0）も 0。10 にはならない
        assertThat(CheckDigit.calculate("0000000")).isEqualTo('0');
        assertThat(CheckDigit.isValid("00000000")).isTrue();
        // 合計 9 → 1、合計 1 → 9（境界の前後）
        assertThat(CheckDigit.calculate("3000000")).isEqualTo('1');
        assertThat(CheckDigit.calculate("0000010")).isEqualTo('9');
    }

    static Stream<String> sevenDigitSamples() {
        return Stream.of("1000001", "1000002", "2000001", "2000002", "2000003", "2000004", "2000005",
                "2000006", "2000008", "2000009", "2000010", "9000001", "9000002", "9000003", "1234567", "9999999");
    }

    @ParameterizedTest
    @MethodSource("sevenDigitSamples")
    void matchesFormulaAndExactlyOneDigitIsValid(String first7) {
        int[] w = {3, 1, 3, 1, 3, 1, 3};
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += (first7.charAt(i) - '0') * w[i];
        }
        char expected = (char) ('0' + (10 - sum % 10) % 10);
        assertThat(CheckDigit.calculate(first7)).isEqualTo(expected);
        assertThat(IntStream.rangeClosed(0, 9).filter(d -> CheckDigit.isValid(first7 + d)).count()).isEqualTo(1);
    }

    @Test
    void oldModulus11IsNotUsed() {
        // 旧モジュラス11（ウェイト 7〜1）だと 1000001 は (11 - (7+1) MOD 11) MOD 11 = 3。新方式では不正
        assertThat(CheckDigit.isValid("10000013")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1000001", "100000144", "1000001A", "A0000014", " 0000014", "1000001 ",
            "１０００００１４", "-1000001", "1000.014"})
    void nonEightAsciiDigitsAreInvalid(String itemCd) {
        // カタログ D-02：数字以外を含む・桁数不正は M001（異常終了は再現しない）
        assertThat(CheckDigit.isValid(itemCd)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"100000", "10000014", "100000A", "１００００００"})
    void calculateRejectsInvalidInput(String first7) {
        assertThatThrownBy(() -> CheckDigit.calculate(first7)).isInstanceOf(IllegalArgumentException.class);
    }
}
