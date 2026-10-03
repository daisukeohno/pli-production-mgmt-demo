package jp.sanseki.pm.pm01;

import java.util.List;

/**
 * SEND MAP 相当の出力。
 * msgId / msgText / fieldPos は F-MSG（23 行目）と CA-MSG-FLD-POS（カーソル位置）に対応する。
 */
public record Pm01Response(
        String scrnId,
        /** 1 行目右端の日付 YYYY/MM/DD */ String sysDate,
        String msgId,
        String msgText,
        String fieldPos,
        Fields fields,
        /** F-LIST01〜F-LIST10 */ List<ListRow> list,
        int pageNo,
        /** PF3 で疑似会話を終了した */ boolean ended) {

    public record Fields(String func, String itemCd, String itemName, String itemKbn, String stockUnit) {
    }

    public record ListRow(String itemCd, String itemName, String itemKbn) {
    }
}
