package jp.sanseki.pm.pm02;

/**
 * PM02 BMS マップの項目（定義順＝BMS 項目順）。
 * {@code order} が CA-MSG-FLD-POS に格納する値、{@code bmsName} が API の fieldPos 識別子。
 */
public enum Pm02Field {
    F_ITEMCD("F-ITEMCD", 1, 8),
    F_ITEMNM("F-ITEMNM", 2, 20),
    F_STOCKQTY("F-STOCKQTY", 3, 9),
    F_IOKBN("F-IOKBN", 4, 1),
    F_IOQTY("F-IOQTY", 5, 9),
    F_MSG("F-MSG", 6, 60);

    private final String bmsName;
    private final short order;
    private final int length;

    Pm02Field(String bmsName, int order, int length) {
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

    public static Pm02Field ofBmsName(String bmsName) {
        for (Pm02Field f : values()) {
            if (f.bmsName.equals(bmsName)) {
                return f;
            }
        }
        return null;
    }
}
