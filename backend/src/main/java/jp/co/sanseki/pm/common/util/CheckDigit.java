package jp.co.sanseki.pm.common.util;

/**
 * 品目コード チェックデジット（PMUTL01 CALC_CHK_DGT / VALID-CHK-DGT の移植）。
 *
 * <p>モジュラス10・ウェイト3-1：先頭7桁の各桁に左から 3,1,3,1,3,1,3 を乗じて合計し、
 * チェックデジット = (10 - (合計 MOD 10)) MOD 10。2003-02-10 に廃止された旧モジュラス11方式は移植しない。
 */
public final class CheckDigit {

    public static final int ITEM_CD_LENGTH = 8;
    private static final int[] WEIGHTS = {3, 1, 3, 1, 3, 1, 3};

    private CheckDigit() {
    }

    /** P-FUNC='C'：先頭7桁からチェックデジットを計算する。 */
    public static char calculate(String itemCd7) {
        if (itemCd7 == null || itemCd7.length() != WEIGHTS.length || !isDigits(itemCd7)) {
            throw new IllegalArgumentException("品目コード先頭7桁は数字7桁で指定してください: " + itemCd7);
        }
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (itemCd7.charAt(i) - '0') * WEIGHTS[i];
        }
        return (char) ('0' + (10 - sum % 10) % 10);
    }

    /** P-FUNC='V'：8桁の品目コードの末尾がチェックデジットと一致すれば true（P-VALID-FLG='Y'）。 */
    public static boolean isValid(String itemCd8) {
        if (itemCd8 == null || itemCd8.length() != ITEM_CD_LENGTH || !isDigits(itemCd8)) {
            return false;
        }
        return calculate(itemCd8.substring(0, 7)) == itemCd8.charAt(7);
    }

    private static boolean isDigits(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
