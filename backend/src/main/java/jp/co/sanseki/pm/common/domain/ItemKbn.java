package jp.co.sanseki.pm.common.domain;

import java.util.Arrays;
import java.util.Optional;

/** 品目区分（DCL_ITEM_MST.inc：ITEM-KBN-PRODUCT/PART/SUPPLY）。PM01〜PM03 共通。 */
public enum ItemKbn {
    PRODUCT("1", "製品"),
    PART("2", "部品"),
    SUPPLY("9", "消耗品");

    private final String code;
    private final String label;

    ItemKbn(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static Optional<ItemKbn> of(String code) {
        return Arrays.stream(values()).filter(k -> k.code.equals(code)).findFirst();
    }
}
