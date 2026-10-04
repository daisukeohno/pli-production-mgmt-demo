package jp.sanseki.pm.pm02;

import java.math.BigDecimal;

/**
 * PM02 の照会結果（ITEM_MST の有効品目 + STOCK）。CHAR 列は末尾空白除去済。
 *
 * @param stockQty STOCK 行が無い場合は null
 */
public record StockItem(String itemCd, String itemName, String itemKbn, BigDecimal stockQty) {

    public static final int ITEM_CD_LEN = 8;

    /** 品目区分 9:消耗品（マイナス在庫を許容する唯一の区分） */
    public static final String ITEM_KBN_SUPPLY = "9";

    /** STOCK_QTY DEC(9,0) の上限・下限 */
    public static final BigDecimal STOCK_QTY_MAX = new BigDecimal("999999999");
    public static final BigDecimal STOCK_QTY_MIN = STOCK_QTY_MAX.negate();

    public BigDecimal stockQtyOrZero() {
        return stockQty == null ? BigDecimal.ZERO : stockQty;
    }
}
