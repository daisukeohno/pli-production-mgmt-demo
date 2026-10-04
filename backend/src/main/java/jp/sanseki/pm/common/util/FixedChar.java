package jp.sanseki.pm.common.util;

/**
 * DB2 固定長 CHAR(n) の空白パディング挙動に合わせた文字列操作。
 *
 * <ul>
 *   <li>読込・入力時: 末尾空白（BMS の空白/LOW-VALUE 埋め相当）を除去して扱う。先頭空白は保持する。</li>
 *   <li>書込時: CHAR(n) と同じく n 文字まで空白で右詰めパディングする。</li>
 *   <li>比較: 末尾空白を除去した値同士で比較する（DB2 の CHAR 比較は短い側を空白補完して比較するため等価）。</li>
 * </ul>
 * 桁数は文字数（コードポイント数）で数える。
 */
public final class FixedChar {

    private FixedChar() {
    }

    public static String rtrim(String s) {
        if (s == null) {
            return "";
        }
        int end = s.length();
        while (end > 0 && (s.charAt(end - 1) == ' ' || s.charAt(end - 1) == '\u0000')) {
            end--;
        }
        return s.substring(0, end);
    }

    public static boolean isBlank(String s) {
        return rtrim(s).isEmpty();
    }

    public static int length(String s) {
        return s == null ? 0 : s.codePointCount(0, s.length());
    }

    public static boolean fits(String s, int len) {
        return length(rtrim(s)) <= len;
    }

    public static String pad(String s, int len) {
        String v = rtrim(s);
        int l = length(v);
        if (l > len) {
            throw new IllegalArgumentException("CHAR(" + len + ") 桁あふれ: " + v);
        }
        return v + " ".repeat(len - l);
    }

    public static boolean equalsChar(String a, String b) {
        return rtrim(a).equals(rtrim(b));
    }
}
