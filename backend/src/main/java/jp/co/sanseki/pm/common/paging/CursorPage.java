package jp.co.sanseki.pm.common.paging;

import java.util.List;
import java.util.function.Function;

/**
 * キーセット方式の1頁分。topKey は当該頁の先頭キー（CA01-TOP-ITEM-CD 相当）、pageNo は表示頁（CA01-PAGE-NO 相当）。
 * 次の PF7/PF8 では topKey・pageNo をそのまま送り返す。
 */
public record CursorPage<T>(List<T> items, int pageNo, String topKey, boolean hasPrev, boolean hasNext, int size) {

    public <R> CursorPage<R> map(Function<? super T, ? extends R> mapper) {
        return new CursorPage<>(items.stream().<R>map(mapper).toList(), pageNo, topKey, hasPrev, hasNext, size);
    }
}
