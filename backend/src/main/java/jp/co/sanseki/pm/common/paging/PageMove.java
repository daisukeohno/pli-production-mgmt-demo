package jp.co.sanseki.pm.common.paging;

import java.util.Locale;

/** 一覧のページ移動。PF7=PREV、PF8=NEXT、再表示=CURRENT、初回表示=FIRST。 */
public enum PageMove {
    FIRST, CURRENT, NEXT, PREV;

    public static PageMove parse(String value) {
        if (value == null || value.isBlank()) {
            return FIRST;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
