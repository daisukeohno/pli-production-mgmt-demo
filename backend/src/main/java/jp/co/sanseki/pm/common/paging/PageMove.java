package jp.co.sanseki.pm.common.paging;

import java.util.Locale;

/** 頁移動。PF7=前頁（PREV）、PF8=次頁（NEXT）、CURRENT=カーソル位置の頁を再表示。 */
public enum PageMove {
    CURRENT,
    PREV,
    NEXT;

    public static PageMove of(String value) {
        if (value == null || value.isBlank()) {
            return CURRENT;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "PREV", "PF7" -> PREV;
            case "NEXT", "PF8" -> NEXT;
            case "CURRENT" -> CURRENT;
            default -> throw new IllegalArgumentException("move は prev/next/current のいずれかです: " + value);
        };
    }
}
