package jp.sanseki.pm.pm01;

import jp.sanseki.pm.common.web.Aid;

/** RECEIVE MAP 相当の入力（注意キー＋入力項目）。 */
public record Pm01Request(
        Aid aid,
        /** F-FUNC */ String func,
        /** F-ITEMCD */ String itemCd,
        /** F-ITEMNM */ String itemName,
        /** F-KBN */ String itemKbn,
        /** F-UNIT */ String stockUnit) {
}
