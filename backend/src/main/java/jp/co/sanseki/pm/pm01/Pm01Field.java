package jp.co.sanseki.pm.pm01;

import jp.co.sanseki.pm.common.error.ScreenField;

/** PM01 の入力項目（BMS 項目順。position が CA-MSG-FLD-POS）。 */
public enum Pm01Field implements ScreenField {
    FUNC("funcCd", "F-FUNC", 1),
    ITEM_CD("itemCd", "F-ITEMCD", 2),
    ITEM_NAME("itemName", "F-ITEMNM", 3),
    ITEM_KBN("itemKbn", "F-KBN", 4),
    STOCK_UNIT("stockUnit", "F-UNIT", 5);

    private final String fieldName;
    private final String bmsName;
    private final int position;

    Pm01Field(String fieldName, String bmsName, int position) {
        this.fieldName = fieldName;
        this.bmsName = bmsName;
        this.position = position;
    }

    @Override
    public String fieldName() {
        return fieldName;
    }

    @Override
    public String bmsName() {
        return bmsName;
    }

    @Override
    public int position() {
        return position;
    }
}
