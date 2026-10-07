package jp.co.sanseki.pm.workorder;

import java.util.Optional;

import jp.co.sanseki.pm.common.Chars;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class WorkOrderRepository {

    static final RowMapper<WorkOrder> ROW_MAPPER = (rs, i) -> new WorkOrder(
            Chars.trimPadding(rs.getString("WORK_ORDER_NO")),
            Chars.trimPadding(rs.getString("ITEM_CD")),
            rs.getBigDecimal("ORDER_QTY"),
            rs.getDate("DUE_DATE").toLocalDate(),
            Chars.trimPadding(rs.getString("STATUS")),
            rs.getTimestamp("CRT_TMS").toLocalDateTime());

    private final JdbcClient jdbc;

    public WorkOrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<WorkOrder> findByWorkOrderNo(String workOrderNo) {
        return jdbc.sql("""
                SELECT WORK_ORDER_NO, ITEM_CD, ORDER_QTY, DUE_DATE, STATUS, CRT_TMS
                  FROM SANSEKI.WORK_ORDER
                 WHERE WORK_ORDER_NO = :workOrderNo
                """)
                .param("workOrderNo", workOrderNo)
                .query(ROW_MAPPER)
                .optional();
    }
}
