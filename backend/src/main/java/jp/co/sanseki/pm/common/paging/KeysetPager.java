package jp.co.sanseki.pm.common.paging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 頁先頭キー（COMMAREA の CAnn-TOP-xxx）を持ち回る疑似会話型ページングの再現。
 * <ul>
 *   <li>CURRENT：cursor 以上を size 件（cursor 未指定は先頭頁）</li>
 *   <li>NEXT（PF8）：現頁の最終キーより後ろを size 件。後ろが無ければ現頁のまま</li>
 *   <li>PREV（PF7）：cursor より前を size 件。前が無ければ現頁のまま、size 件に満たなければ先頭頁</li>
 * </ul>
 */
public class KeysetPager<T> {

    private final KeysetSource<T> source;
    private final int size;

    public KeysetPager(KeysetSource<T> source, int size) {
        this.source = source;
        this.size = size;
    }

    public KeysetPage<T> page(PageMove move, String cursor, int pageNo) {
        int page = Math.max(1, pageNo);
        List<T> current = currentRows(cursor);
        if (current.isEmpty() && cursor != null) {
            // 頁先頭以降が全件削除された場合は直前の頁を表示する
            List<T> before = ascending(source.beforeKey(cursor, size));
            return result(before.isEmpty() ? source.first(size) : before, Math.max(1, page - 1));
        }
        return switch (move) {
            case CURRENT -> result(current, page);
            case NEXT -> {
                if (current.isEmpty()) {
                    yield result(current, page);
                }
                List<T> next = source.afterKey(source.keyOf(current.get(current.size() - 1)), size);
                yield next.isEmpty() ? result(current, page) : result(next, page + 1);
            }
            case PREV -> {
                if (current.isEmpty()) {
                    yield result(current, 1);
                }
                List<T> prev = ascending(source.beforeKey(source.keyOf(current.get(0)), size));
                if (prev.isEmpty()) {
                    yield result(current, 1);
                }
                yield prev.size() < size ? result(source.first(size), 1) : result(prev, Math.max(1, page - 1));
            }
        };
    }

    private List<T> currentRows(String cursor) {
        return cursor == null || cursor.isBlank() ? source.first(size) : source.fromKey(cursor, size);
    }

    private KeysetPage<T> result(List<T> rows, int pageNo) {
        if (rows.isEmpty()) {
            return new KeysetPage<>(rows, null, 1, false, false);
        }
        String top = source.keyOf(rows.get(0));
        String last = source.keyOf(rows.get(rows.size() - 1));
        boolean hasPrev = !source.beforeKey(top, 1).isEmpty();
        boolean hasNext = !source.afterKey(last, 1).isEmpty();
        return new KeysetPage<>(rows, top, hasPrev ? pageNo : 1, hasPrev, hasNext);
    }

    private static <T> List<T> ascending(List<T> descending) {
        List<T> list = new ArrayList<>(descending);
        Collections.reverse(list);
        return list;
    }
}
