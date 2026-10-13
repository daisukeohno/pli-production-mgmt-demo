package jp.co.sanseki.pm.common.paging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 「当該頁の先頭キーを保持して次頁／前頁」を行う汎用ページャ（PMCOMM.inc CA01-PAGE-NO / CA01-TOP-ITEM-CD の動作）。
 *
 * <p>先頭頁で PF7、最終頁で PF8 を押した場合は頁を移動せず同じ頁を返す。
 */
public final class KeysetPager<T> {

    /** キー昇順で fromInclusive 以上（null なら先頭から）を最大 limit 件。 */
    @FunctionalInterface
    public interface Forward<T> {
        List<T> fetch(String fromInclusive, int limit);
    }

    /** キー降順で beforeExclusive 未満を最大 limit 件。 */
    @FunctionalInterface
    public interface Backward<T> {
        List<T> fetch(String beforeExclusive, int limit);
    }

    private final Forward<T> forward;
    private final Backward<T> backward;
    private final Function<T, String> keyOf;

    public KeysetPager(Forward<T> forward, Backward<T> backward, Function<T, String> keyOf) {
        this.forward = forward;
        this.backward = backward;
        this.keyOf = keyOf;
    }

    public CursorPage<T> page(PageMove move, String topKey, int pageNo, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        int currentPageNo = Math.max(pageNo, 1);
        if (move == PageMove.FIRST || topKey == null || topKey.isBlank()) {
            List<T> rows = forward.fetch(null, size + 1);
            return build(head(rows, size), 1, false, rows.size() > size, size);
        }
        return switch (move) {
            case NEXT -> next(topKey, currentPageNo, size);
            case PREV -> prev(topKey, currentPageNo, size);
            default -> current(topKey, currentPageNo, size);
        };
    }

    private CursorPage<T> current(String topKey, int pageNo, int size) {
        List<T> rows = forward.fetch(topKey, size + 1);
        if (rows.isEmpty()) {
            // 当該頁の品目がすべて削除された場合は前頁を表示する
            List<T> before = backward.fetch(topKey, size);
            if (before.isEmpty()) {
                return build(List.of(), 1, false, false, size);
            }
            List<T> page = reversed(before);
            return build(page, Math.max(pageNo - 1, 1), hasBefore(page), false, size);
        }
        return build(head(rows, size), pageNo, !backward.fetch(topKey, 1).isEmpty(), rows.size() > size, size);
    }

    private CursorPage<T> next(String topKey, int pageNo, int size) {
        List<T> rows = forward.fetch(topKey, size * 2 + 1);
        if (rows.size() <= size) {
            return current(topKey, pageNo, size);
        }
        List<T> page = rows.subList(size, Math.min(rows.size(), size * 2));
        return build(page, pageNo + 1, true, rows.size() > size * 2, size);
    }

    private CursorPage<T> prev(String topKey, int pageNo, int size) {
        List<T> before = backward.fetch(topKey, size);
        if (before.isEmpty()) {
            return current(topKey, pageNo, size);
        }
        List<T> page = reversed(before);
        return build(page, Math.max(pageNo - 1, 1), hasBefore(page), true, size);
    }

    private boolean hasBefore(List<T> page) {
        return !backward.fetch(keyOf.apply(page.get(0)), 1).isEmpty();
    }

    private CursorPage<T> build(List<T> items, int pageNo, boolean hasPrev, boolean hasNext, int size) {
        String top = items.isEmpty() ? null : keyOf.apply(items.get(0));
        return new CursorPage<>(List.copyOf(items), pageNo, top, hasPrev, hasNext, size);
    }

    private static <T> List<T> head(List<T> rows, int size) {
        return rows.size() > size ? rows.subList(0, size) : rows;
    }

    private static <T> List<T> reversed(List<T> rows) {
        List<T> copy = new ArrayList<>(rows);
        Collections.reverse(copy);
        return copy;
    }
}
