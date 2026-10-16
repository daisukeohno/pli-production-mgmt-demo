package jp.co.sanseki.pm.common.paging;

import java.util.List;
import java.util.function.Function;

/**
 * キーセット方式の1頁分。
 *
 * @param rows    表示行
 * @param cursor  当該頁の先頭キー（CA01-TOP-ITEM-CD 相当。次回の PF7/PF8 で送り返す）
 * @param pageNo  表示頁（CA01-PAGE-NO 相当）
 * @param hasPrev 前頁があるか
 * @param hasNext 次頁があるか
 */
public record KeysetPage<T>(List<T> rows, String cursor, int pageNo, boolean hasPrev, boolean hasNext) {

    public <R> KeysetPage<R> map(Function<T, R> mapper) {
        return new KeysetPage<>(rows.stream().map(mapper).toList(), cursor, pageNo, hasPrev, hasNext);
    }
}
