package jp.sanseki.pm.pm01;

/**
 * PM01 BMS マップの項目（定義順＝BMS 項目順）。
 * {@code order} が CA-MSG-FLD-POS に格納する値、{@code bmsName} が API の fieldPos 識別子。
 */
public enum Pm01Field {
    F_FUNC("F-FUNC", 1, 1),
    F_ITEMCD("F-ITEMCD", 2, 8),
    F_ITEMNM("F-ITEMNM", 3, 20),
    F_KBN("F-KBN", 4, 1),
    F_UNIT("F-UNIT", 5, 4),
    F_LIST01("F-LIST01", 6, 33),
    F_LIST02("F-LIST02", 7, 33),
    F_LIST03("F-LIST03", 8, 33),
    F_LIST04("F-LIST04", 9, 33),
    F_LIST05("F-LIST05", 10, 33),
    F_LIST06("F-LIST06", 11, 33),
    F_LIST07("F-LIST07", 12, 33),
    F_LIST08("F-LIST08", 13, 33),
    F_LIST09("F-LIST09", 14, 33),
    F_LIST10("F-LIST10", 15, 33),
    F_MSG("F-MSG", 16, 60);

    private final String bmsName;
    private final short order;
    private final int length;

    Pm01Field(String bmsName, int order, int length) {
        this.bmsName = bmsName;
        this.order = (short) order;
        this.length = length;
    }

    public String bmsName() {
        return bmsName;
    }

    public short order() {
        return order;
    }

    public int length() {
        return length;
    }

    public static Pm01Field ofOrder(short order) {
        for (Pm01Field f : values()) {
            if (f.order == order) {
                return f;
            }
        }
        return null;
    }
}
