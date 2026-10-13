package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import jp.co.sanseki.pm.common.paging.CursorPage;
import jp.co.sanseki.pm.common.paging.KeysetPager;
import jp.co.sanseki.pm.common.paging.PageMove;
import org.junit.jupiter.api.Test;

class KeysetPagerTest {

    private final List<String> keys = IntStream.rangeClosed(1, 25).mapToObj(i -> String.format("K%03d", i)).toList();

    private final KeysetPager<String> pager = new KeysetPager<>(
            (from, limit) -> keys.stream().filter(k -> from == null || k.compareTo(from) >= 0).limit(limit).toList(),
            (before, limit) -> keys.stream().sorted(java.util.Comparator.reverseOrder()).filter(k -> k.compareTo(before) < 0).limit(limit).toList(),
            k -> k);

    @Test
    void firstNextNextPrev() {
        CursorPage<String> p1 = pager.page(PageMove.FIRST, null, 1, 10);
        assertThat(p1.items()).containsExactlyElementsOf(keys.subList(0, 10));
        assertThat(p1).extracting(CursorPage::pageNo, CursorPage::topKey, CursorPage::hasPrev, CursorPage::hasNext)
                .containsExactly(1, "K001", false, true);

        CursorPage<String> p2 = pager.page(PageMove.NEXT, p1.topKey(), p1.pageNo(), 10);
        assertThat(p2.items()).containsExactlyElementsOf(keys.subList(10, 20));
        assertThat(p2).extracting(CursorPage::pageNo, CursorPage::hasPrev, CursorPage::hasNext).containsExactly(2, true, true);

        CursorPage<String> p3 = pager.page(PageMove.NEXT, p2.topKey(), p2.pageNo(), 10);
        assertThat(p3.items()).containsExactlyElementsOf(keys.subList(20, 25));
        assertThat(p3).extracting(CursorPage::pageNo, CursorPage::hasPrev, CursorPage::hasNext).containsExactly(3, true, false);

        CursorPage<String> stay = pager.page(PageMove.NEXT, p3.topKey(), p3.pageNo(), 10);
        assertThat(stay).isEqualTo(p3);

        CursorPage<String> back = pager.page(PageMove.PREV, p3.topKey(), p3.pageNo(), 10);
        assertThat(back).isEqualTo(p2);
    }

    @Test
    void prevOnFirstPageStays() {
        CursorPage<String> p1 = pager.page(PageMove.FIRST, null, 1, 10);
        assertThat(pager.page(PageMove.PREV, p1.topKey(), 1, 10)).isEqualTo(p1);
    }

    @Test
    void currentStartsFromTopKeyEvenIfTopWasRemoved() {
        CursorPage<String> page = pager.page(PageMove.CURRENT, "K010A", 2, 10);
        assertThat(page.items()).first().isEqualTo("K011");
        assertThat(page.hasPrev()).isTrue();
    }
}
