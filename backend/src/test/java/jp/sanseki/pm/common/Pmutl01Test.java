package jp.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import jp.sanseki.pm.common.util.Pmutl01;

class Pmutl01Test {

    @Test
    void calcChkDgtIsModulus10Weight31() {
        // 1*3 + 0 + 0 + 0 + 0 + 0 + 1*3 = 6 → (10 - 6) mod 10 = 4
        assertThat(Pmutl01.calcChkDgt("1000001")).isEqualTo('4');
        // 合計が 10 の倍数 → 0
        assertThat(Pmutl01.calcChkDgt("0000000")).isEqualTo('0');
        assertThat(Pmutl01.calcChkDgt("3000001")).isEqualTo('8');
    }

    @Test
    void allInitialItemCodesAreValid() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("../db2/data/item_mst.csv"));
        for (String line : lines.subList(1, lines.size())) {
            String cd = line.split(",")[0];
            assertThat(Pmutl01.validChkDgt(cd)).as(cd).isTrue();
        }
    }

    @Test
    void legacyModulus11IsNotAccepted() {
        // 旧モジュラス11（ウェイト 7..1）では 3000001 → 0。現行方式では不正。
        assertThat(Pmutl01.validChkDgt("30000010")).isFalse();
        assertThat(Pmutl01.validChkDgt("30000018")).isTrue();
    }

    @Test
    void invalidFormatIsNotValid() {
        assertThat(Pmutl01.validChkDgt("ABCDEFGH")).isFalse();
        assertThat(Pmutl01.validChkDgt("1000001")).isFalse();
        assertThat(Pmutl01.validChkDgt(null)).isFalse();
        assertThatThrownBy(() -> Pmutl01.calcChkDgt("10000A1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sysDateIsJst() {
        // 2026-10-03T15:30Z = 2026-10-04 00:30 JST
        Clock clock = Clock.fixed(Instant.parse("2026-10-03T15:30:00Z"), ZoneId.of("Asia/Tokyo"));
        assertThat(new Pmutl01(clock).getSysDate()).isEqualTo("2026-10-04");
    }
}
