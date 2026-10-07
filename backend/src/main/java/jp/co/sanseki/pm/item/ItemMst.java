package jp.co.sanseki.pm.item;

import java.time.LocalDateTime;

/** SANSEKI.ITEM_MST の 1 行（CHAR 列は末尾パディングを除いた値）。 */
public record ItemMst(
        String itemCd,
        String itemName,
        String itemKbn,
        String stockUnit,
        String delFlg,
        LocalDateTime updTms,
        LocalDateTime crtTms) {

    public static final String KBN_PRODUCT = "1";
    public static final String KBN_PART = "2";
    public static final String KBN_SUPPLY = "9";
    public static final String DEL_FLG_ACTIVE = "0";
    public static final String DEL_FLG_DELETED = "1";

    public boolean isDeleted() {
        return DEL_FLG_DELETED.equals(delFlg);
    }
}
