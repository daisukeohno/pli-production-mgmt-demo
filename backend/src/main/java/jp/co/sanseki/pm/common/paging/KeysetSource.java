package jp.co.sanseki.pm.common.paging;

import java.util.List;

/** キーセット方式の頁読み出し（キー昇順）。画面ごとのリポジトリが実装する。 */
public interface KeysetSource<T> {

    /** 先頭から limit 件。 */
    List<T> first(int limit);

    /** key 以上を昇順に limit 件。 */
    List<T> fromKey(String key, int limit);

    /** key より大きいものを昇順に limit 件。 */
    List<T> afterKey(String key, int limit);

    /** key より小さいものを降順に limit 件。 */
    List<T> beforeKey(String key, int limit);

    String keyOf(T row);
}
