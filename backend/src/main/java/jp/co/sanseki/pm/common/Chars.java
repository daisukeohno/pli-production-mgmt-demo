package jp.co.sanseki.pm.common;

/**
 * CHAR(n) 固定長列の値の扱い。DB から読んだ値は末尾の半角空白（パディング）だけを除く（カタログ §5）。
 * 全角空白はパディングではないので残す。
 */
public final class Chars {

    private Chars() {
    }

    public static String trimPadding(String value) {
        if (value == null) {
            return null;
        }
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == ' ') {
            end--;
        }
        return value.substring(0, end);
    }
}
