package jp.sanseki.pm.pm03;

/**
 * PM03 BMS マップの項目（定義順＝BMS 項目順）。
 * {@code order} が CA-MSG-FLD-POS に格納する値、{@code bmsName} が API の fieldPos 識別子。
 */
public enum Pm03Field {
    F_ITEMCD("F-ITEMCD", 1, 8),
    F_ITEMNM("F-ITEMNM", 2, 20),
    F_ORDERQTY("F-ORDERQTY", 3, 9),
    F_DUEDATE("F-DUEDATE", 4, 10),
    F_ORDERNO("F-ORDERNO", 5, 7),
    F_NGLIST01("F-NGLIST01", 6, 30),
    F_NGLIST02("F-NGLIST02", 7, 30),
    F_NGLIST03("F-NGLIST03", 8, 30),
    F_NGLIST04("F-NGLIST04", 9, 30),
    F_NGLIST05("F-NGLIST05", 10, 30),
    F_NGLIST06("F-NGLIST06", 11, 30),
    F_NGLIST07("F-NGLIST07", 12, 30),
    F_NGLIST08("F-NGLIST08", 13, 30),
    F_NGLIST09("F-NGLIST09", 14, 30),
    F_NGLIST10("F-NGLIST10", 15, 30),
    F_MSG("F-MSG", 16, 60);

    private final String bmsName;
    private final short order;
    private final int length;

    Pm03Field(String bmsName, int order, int length) {
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

    public static Pm03Field ofOrder(short order) {
        for (Pm03Field f : values()) {
            if (f.order == order) {
                return f;
            }
        }
        return null;
    }
}
