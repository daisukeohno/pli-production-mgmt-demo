package jp.sanseki.pm.pm02;

import jp.sanseki.pm.common.web.Aid;

/** RECEIVE MAP 相当の入力（注意キー＋入力項目）。 */
public record Pm02Request(
        Aid aid,
        /** F-ITEMCD */ String itemCd,
        /** F-IOKBN */ String ioKbn,
        /** F-IOQTY */ String ioQty) {
}
