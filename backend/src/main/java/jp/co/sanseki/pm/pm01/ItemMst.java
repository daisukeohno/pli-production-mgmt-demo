package jp.co.sanseki.pm.pm01;

import java.time.LocalDateTime;

/** ITEM_MST の1行（DCLITEM-MST 相当）。CHAR 項目は末尾空白を除いた値で保持する。 */
public record ItemMst(String itemCd, String itemName, String itemKbn, String stockUnit, String delFlg,
                      LocalDateTime updTms, LocalDateTime crtTms) {

    public static final int ITEM_CD_LEN = 8;
    public static final int ITEM_NAME_LEN = 20;
    public static final int ITEM_KBN_LEN = 1;
    public static final int STOCK_UNIT_LEN = 4;

    public static final String DEL_FLG_ACTIVE = "0";
    public static final String DEL_FLG_DELETED = "1";
}
