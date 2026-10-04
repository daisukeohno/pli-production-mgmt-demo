package jp.sanseki.pm.pm03;

import java.math.BigDecimal;
import java.util.List;

/**
 * SEND MAP 相当の出力。
 * msgId / msgText / fieldPos は F-MSG（23 行目）と CA-MSG-FLD-POS（カーソル位置）に対応する。
 * ngList は F-NGLIST01〜F-NGLIST10 の表示内容。PF4 でのみ送信され、それ以外の送信では空。
 */
public record Pm03Response(
        String scrnId,
        /** 1 行目右端の日付 YYYY/MM/DD */ String sysDate,
        String msgId,
        String msgText,
        String fieldPos,
        Fields fields,
        /** F-NGLIST01〜F-NGLIST10（PF4 で表示する在庫不足部品一覧、未表示時は空） */ List<NgRow> ngList,
        /** PF3 で疑似会話を終了した */ boolean ended) {

    public record Fields(String itemCd, String itemName, String orderQty, String dueDate, String orderNo) {
    }

    public record NgRow(String itemCd, BigDecimal needQty, BigDecimal stockQty) {
    }
}
