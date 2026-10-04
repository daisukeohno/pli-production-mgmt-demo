package jp.sanseki.pm.common.util;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

/**
 * PMUTL01 共通ユーティリティの移植。
 *
 * <ul>
 *   <li>CALC_CHK_DGT : モジュラス10・ウェイト3-1（7桁に左から 3,1,3,1,3,1,3 を乗算）</li>
 *   <li>VALID-CHK-DGT: 先頭7桁から計算した値と末尾1桁の照合</li>
 *   <li>GET_SYS_DATE : EXEC CICS ASKTIME / FORMATTIME YYYYMMDD DATESEP('-') 相当（JST 固定）</li>
 * </ul>
 * 2003-02-10 に廃止された旧モジュラス11方式は移植しない。
 */
@Component
public class Pmutl01 {

    private static final int[] WK_WEIGHT = {3, 1, 3, 1, 3, 1, 3};

    private final Clock clock;

    public Pmutl01(Clock clock) {
        this.clock = clock;
    }

    /**
     * CALC_CHK_DGT。先頭7桁（数字）からチェックデジットを計算する。
     *
     * @throws IllegalArgumentException 7桁の数字でない場合（PL/I では CONVERSION 条件に相当）
     */
    public static char calcChkDgt(String cd7) {
        if (cd7 == null || cd7.length() != 7 || !cd7.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw new IllegalArgumentException("品目コード先頭7桁が数字ではありません: " + cd7);
        }
        int wkSum = 0;
        for (int i = 0; i < 7; i++) {
            wkSum += (cd7.charAt(i) - '0') * WK_WEIGHT[i];
        }
        int wkMod = wkSum % 10;
        int wkChk = (10 - wkMod) % 10;
        return (char) ('0' + wkChk);
    }

    /** VALID-CHK-DGT。8桁の品目コード末尾1桁がチェックデジットと一致すれば true（P-VALID-FLG='Y'）。 */
    public static boolean validChkDgt(String cd8) {
        if (cd8 == null || cd8.length() != 8) {
            return false;
        }
        try {
            return calcChkDgt(cd8.substring(0, 7)) == cd8.charAt(7);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** GET_SYS_DATE。システム日付を 'YYYY-MM-DD'（CHAR(10)）で返す。 */
    public String getSysDate() {
        return Db2Timestamp.DATE_FORMAT.format(ZonedDateTime.now(clock));
    }

    /** CURRENT TIMESTAMP 相当（JST、マイクロ秒精度）。 */
    public LocalDateTime currentTimestamp() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
