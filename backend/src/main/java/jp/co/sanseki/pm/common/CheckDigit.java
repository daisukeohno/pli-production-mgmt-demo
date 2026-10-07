package jp.co.sanseki.pm.common;

/**
 * 品目コードのチェックデジット（PMUTL01 CALC_CHK_DGT / VALID-CHK-DGT の移植）。
 * <p>
 * モジュラス10・ウェイト3-1：先頭 7 桁の各桁に左から 3,1,3,1,3,1,3 を掛けて合計し、
 * チェックデジット = (10 - (合計 MOD 10)) MOD 10。8 桁目と一致すれば正常（カタログ R1）。
 * 旧モジュラス11方式は 2003 年に廃止済みのため実装しない。
 * 旧側の {@code CHAR(WK-CHK,1)} が空白になり得る問題（カタログ D-01）は再現せず、意図どおりの算術で実装する。
 */
public final class CheckDigit {

    public static final int ITEM_CD_LENGTH = 8;
    private static final int BODY_LENGTH = ITEM_CD_LENGTH - 1;
    private static final int[] WEIGHTS = {3, 1, 3, 1, 3, 1, 3};

    private CheckDigit() {
    }

    /** PMUTL01 P-FUNC='C'：先頭 7 桁からチェックデジットを計算する。 */
    public static char calculate(String first7) {
        if (first7 == null || first7.length() != BODY_LENGTH || !isAsciiDigits(first7)) {
            throw new IllegalArgumentException("品目コード先頭7桁は半角数字7桁で指定してください: " + first7);
        }
        int sum = 0;
        for (int i = 0; i < BODY_LENGTH; i++) {
            sum += (first7.charAt(i) - '0') * WEIGHTS[i];
        }
        return (char) ('0' + (10 - sum % 10) % 10);
    }

    /**
     * PMUTL01 P-FUNC='V'：品目コード 8 桁を検証する。
     * 半角数字 8 桁でない値は旧側では CONVERSION 条件になり得るが、新システムでは不正（M001）として扱う（カタログ D-02）。
     */
    public static boolean isValid(String itemCd) {
        if (itemCd == null || itemCd.length() != ITEM_CD_LENGTH || !isAsciiDigits(itemCd)) {
            return false;
        }
        return calculate(itemCd.substring(0, BODY_LENGTH)) == itemCd.charAt(BODY_LENGTH);
    }

    private static boolean isAsciiDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
