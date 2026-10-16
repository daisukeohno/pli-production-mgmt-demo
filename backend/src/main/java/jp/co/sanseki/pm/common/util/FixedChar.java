package jp.co.sanseki.pm.common.util;

/**
 * DB2 CHAR(n) 固定長項目の扱い。
 * DB には空白埋めのまま格納し（PostgreSQL bpchar）、アプリ層では末尾空白を除いた値で扱う。
 * 桁数は文字数で判定する（BMS 項目長 = CHAR 長）。
 */
public final class FixedChar {

    private FixedChar() {
    }

    /** 末尾の半角空白（CHAR の埋め文字）を除去する。null は空文字。 */
    public static String rtrim(String value) {
        if (value == null) {
            return "";
        }
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == ' ') {
            end--;
        }
        return value.substring(0, end);
    }

    /** 空白のみ・未入力（BMS の未入力・空白入力）。 */
    public static boolean isBlank(String value) {
        return rtrim(value).isEmpty();
    }

    /** 末尾空白を除いた値が CHAR(n) に収まるか。 */
    public static boolean fits(String value, int length) {
        String v = rtrim(value);
        return v.codePointCount(0, v.length()) <= length;
    }

    /** CHAR(n) と同じく右側を空白で埋める（帳票・一覧の固定長表示用）。 */
    public static String pad(String value, int length) {
        String v = rtrim(value);
        int len = v.codePointCount(0, v.length());
        return len >= length ? v : v + " ".repeat(length - len);
    }
}
