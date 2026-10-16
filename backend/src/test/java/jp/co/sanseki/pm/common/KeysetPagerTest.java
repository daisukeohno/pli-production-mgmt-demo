package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import jp.co.sanseki.pm.common.paging.KeysetPage;
import jp.co.sanseki.pm.common.paging.KeysetPager;
import jp.co.sanseki.pm.common.paging.KeysetSource;
import jp.co.sanseki.pm.common.paging.PageMove;
import org.junit.jupiter.api.Test;

class KeysetPagerTest {

    private static KeysetPager<String> pager(int count) {
        List<String> keys = IntStream.rangeClosed(1, count).mapToObj(i -> String.format("K%03d", i)).toList();
        return new KeysetPager<>(new KeysetSource<>() {
            public List<String> first(int limit) {
                return keys.stream().limit(limit).toList();
            }

            public List<String> fromKey(String key, int limit) {
                return keys.stream().filter(k -> k.compareTo(key) >= 0).limit(limit).toList();
            }

            public List<String> afterKey(String key, int limit) {
                return keys.stream().filter(k -> k.compareTo(key) > 0).limit(limit).toList();
            }

            public List<String> beforeKey(String key, int limit) {
                return keys.stream().filter(k -> k.compareTo(key) < 0).sorted(Comparator.reverseOrder()).limit(limit).toList();
            }

            public String keyOf(String row) {
                return row;
            }
        }, 10);
    }

    @Test
    void forwardAndBackward() {
        KeysetPager<String> p = pager(25);
        KeysetPage<String> p1 = p.page(PageMove.CURRENT, null, 1);
        assertThat(p1.rows()).hasSize(10).first().isEqualTo("K001");
        assertThat(p1.hasPrev()).isFalse();
        assertThat(p1.hasNext()).isTrue();

        KeysetPage<String> p2 = p.page(PageMove.NEXT, p1.cursor(), p1.pageNo());
        assertThat(p2.cursor()).isEqualTo("K011");
        assertThat(p2.pageNo()).isEqualTo(2);

        KeysetPage<String> p3 = p.page(PageMove.NEXT, p2.cursor(), p2.pageNo());
        assertThat(p3.rows()).containsExactly("K021", "K022", "K023", "K024", "K025");
        assertThat(p3.pageNo()).isEqualTo(3);
        assertThat(p3.hasNext()).isFalse();

        KeysetPage<String> stay = p.page(PageMove.NEXT, p3.cursor(), p3.pageNo());
        assertThat(stay.cursor()).isEqualTo("K021");
        assertThat(stay.pageNo()).isEqualTo(3);

        KeysetPage<String> back = p.page(PageMove.PREV, p3.cursor(), p3.pageNo());
        assertThat(back.cursor()).isEqualTo("K011");
        assertThat(back.pageNo()).isEqualTo(2);

        KeysetPage<String> top = p.page(PageMove.PREV, back.cursor(), back.pageNo());
        assertThat(top.cursor()).isEqualTo("K001");
        assertThat(top.pageNo()).isEqualTo(1);
        assertThat(p.page(PageMove.PREV, top.cursor(), top.pageNo()).cursor()).isEqualTo("K001");
    }

    @Test
    void prevWithShortRemainderShowsFirstPage() {
        KeysetPage<String> page = pager(25).page(PageMove.PREV, "K006", 2);
        assertThat(page.cursor()).isEqualTo("K001");
        assertThat(page.rows()).hasSize(10);
        assertThat(page.pageNo()).isEqualTo(1);
    }

    @Test
    void empty() {
        KeysetPage<String> page = pager(0).page(PageMove.NEXT, null, 1);
        assertThat(page.rows()).isEmpty();
        assertThat(page.cursor()).isNull();
    }
}
