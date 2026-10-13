package jp.co.sanseki.pm.common.util;

/** DB2 の固定長 CHAR 列（右側空白パディング）を扱うためのユーティリティ。 */
public final class FixedChar {

    private FixedChar() {
    }

    /** 右側の空白パディングを除去する。null は空文字として扱う。 */
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

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
