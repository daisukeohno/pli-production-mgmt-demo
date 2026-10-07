package jp.co.sanseki.pm.workorder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** SANSEKI.BOM の 1 行。QTY_PER DEC(7,2)、YIELD_RATE DEC(5,2)（％値）。 */
public record Bom(
        String parentItemCd,
        String childItemCd,
        BigDecimal qtyPer,
        BigDecimal yieldRate,
        LocalDateTime updTms) {
}
