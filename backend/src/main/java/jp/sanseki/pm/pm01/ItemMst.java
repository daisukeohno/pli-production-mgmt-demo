package jp.sanseki.pm.pm01;

import java.time.LocalDateTime;

/** DCLITEM-MST 相当（CHAR 列は末尾空白除去済）。 */
public record ItemMst(
        String itemCd,
        String itemName,
        String itemKbn,
        String stockUnit,
        String delFlg,
        LocalDateTime updTms,
        LocalDateTime crtTms) {

    public static final int ITEM_CD_LEN = 8;
    public static final int ITEM_NAME_LEN = 20;
    public static final int ITEM_KBN_LEN = 1;
    public static final int STOCK_UNIT_LEN = 4;

    public static final String DEL_FLG_ACTIVE = "0";
    public static final String DEL_FLG_DELETED = "1";

    /** 品目区分 1:製品 2:部品 9:消耗品 */
    public static final String ITEM_KBN_PRODUCT = "1";
    public static final String ITEM_KBN_PART = "2";
    public static final String ITEM_KBN_SUPPLY = "9";

    public static boolean isValidKbn(String kbn) {
        return ITEM_KBN_PRODUCT.equals(kbn) || ITEM_KBN_PART.equals(kbn) || ITEM_KBN_SUPPLY.equals(kbn);
    }
}
