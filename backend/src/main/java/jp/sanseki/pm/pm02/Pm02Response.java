package jp.sanseki.pm.pm02;

import java.math.BigDecimal;

/**
 * SEND MAP 相当の出力。
 * msgId / msgText / fieldPos は F-MSG（23 行目）と CA-MSG-FLD-POS（カーソル位置）に対応する。
 */
public record Pm02Response(
        String scrnId,
        /** 1 行目右端の日付 YYYY/MM/DD */ String sysDate,
        String msgId,
        String msgText,
        String fieldPos,
        Fields fields,
        /** PF3 で疑似会話を終了した */ boolean ended) {

    /**
     * @param itemName F-ITEMNM（照会結果。未照会は空）
     * @param stockQty F-STOCKQTY（照会結果。未照会は null）
     */
    public record Fields(String itemCd, String itemName, BigDecimal stockQty, String ioKbn, String ioQty) {
    }
}
