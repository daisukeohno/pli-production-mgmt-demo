package jp.co.sanseki.pm.common.util;

/**
 * 品目コード チェックデジット（PMUTL01 CALC_CHK_DGT / VALID-CHK-DGT の移植）。
 * モジュラス10・ウェイト3-1：先頭7桁に左から 3,1,3,1,3,1,3 を乗じて合計し、
 * チェックデジット = (10 - (合計 MOD 10)) MOD 10。
 * 旧モジュラス11方式（2003-02-10 廃止、コメントアウト済み）は移植しない。
 */
public final class CheckDigit {

    public static final int BODY_LENGTH = 7;
    public static final int CODE_LENGTH = 8;
    private static final int[] WEIGHTS = {3, 1, 3, 1, 3, 1, 3};

    private CheckDigit() {
    }

    /** P-FUNC='C'：先頭7桁からチェックデジットを計算する。 */
    public static char calc(String body7) {
        if (body7 == null || body7.length() != BODY_LENGTH || !isDigits(body7)) {
            throw new IllegalArgumentException("品目コード先頭7桁は数字7桁で指定してください: " + body7);
        }
        int sum = 0;
        for (int i = 0; i < BODY_LENGTH; i++) {
            sum += (body7.charAt(i) - '0') * WEIGHTS[i];
        }
        int chk = (10 - sum % 10) % 10;
        return (char) ('0' + chk);
    }

    /** P-FUNC='V'：8桁の品目コードの末尾チェックデジットを検証する。数字8桁以外は不正。 */
    public static boolean isValid(String code8) {
        if (code8 == null || code8.length() != CODE_LENGTH || !isDigits(code8)) {
            return false;
        }
        return calc(code8.substring(0, BODY_LENGTH)) == code8.charAt(BODY_LENGTH);
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
