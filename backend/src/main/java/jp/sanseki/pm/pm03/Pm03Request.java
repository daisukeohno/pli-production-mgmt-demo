package jp.sanseki.pm.pm03;

import jp.sanseki.pm.common.web.Aid;

/** RECEIVE MAP 相当の入力（注意キー＋入力項目）。 */
public record Pm03Request(
        Aid aid,
        /** F-ITEMCD 製品コード */ String itemCd,
        /** F-ORDERQTY 指示数量 */ String orderQty,
        /** F-DUEDATE 完成予定日 YYYY-MM-DD */ String dueDate) {
}
