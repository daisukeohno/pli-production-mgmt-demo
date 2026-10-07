package jp.co.sanseki.pm.workorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** SANSEKI.WORK_ORDER の 1 行。STATUS 0:登録済 1:取消済。 */
public record WorkOrder(
        String workOrderNo,
        String itemCd,
        BigDecimal orderQty,
        LocalDate dueDate,
        String status,
        LocalDateTime crtTms) {
}
