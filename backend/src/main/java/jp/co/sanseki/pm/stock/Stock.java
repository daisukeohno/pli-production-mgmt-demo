package jp.co.sanseki.pm.stock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** SANSEKI.STOCK の 1 行。STOCK_QTY DEC(9,0) は BigDecimal（消耗品はマイナス可）。 */
public record Stock(String itemCd, BigDecimal stockQty, LocalDateTime updTms) {
}
