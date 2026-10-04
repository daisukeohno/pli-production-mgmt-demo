package jp.sanseki.pm.common.commarea;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * PMCOMM COMMAREA の移植（PM01/PM02/PM03 共通）。
 * 疑似会話の各トランザクション間で端末（HTTP セッション）単位に保持する。
 * CA01-LOCK-FLG は 2010-06-21 以降のデッドエリアのため移植しない。
 */
public class Commarea implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String FIRST_TIME_YES = "Y";
    public static final String FIRST_TIME_NO = "N";
    public static final int NG_LIST_MAX = 10;

    /** CA-HEADER */
    public final Header header = new Header();
    /** CA-PM01 */
    public final Pm01 pm01 = new Pm01();
    /** CA-PM02 */
    public final Pm02 pm02 = new Pm02();
    /** CA-PM03 */
    public final Pm03 pm03 = new Pm03();
    /** CA-MSG */
    public final Msg msg = new Msg();

    public static class Header implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** CA-SCRN-ID CHAR(4) 直前に表示した画面ID */
        public String scrnId = "";
        /** CA-FUNC-CD CHAR(1) 1:照会 2:登録 3:更新 4:削除 */
        public String funcCd = "";
        /** CA-FIRST-TIME CHAR(1) 'Y':初回起動 'N':2回目以降 */
        public String firstTime = FIRST_TIME_YES;
    }

    public static class Pm01 implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** CA01-ITEM-CD CHAR(8) 選択中（照会済）の品目コード */
        public String itemCd = "";
        /** CA01-PAGE-NO FIXED BIN(15) 現在の表示頁 */
        public short pageNo = 1;
        /** CA01-TOP-ITEM-CD CHAR(8) 当該頁の先頭品目コード（PF7/PF8 用） */
        public String topItemCd = "";
        /** CA01-UPD-TMS CHAR(26) 照会時に読んだ UPD_TMS（楽観排他制御） */
        public String updTms = "";

        public void clearSelection() {
            itemCd = "";
            updTms = "";
        }
    }

    public static class Pm02 implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** CA02-ITEM-CD CHAR(8) */
        public String itemCd = "";
        /** CA02-IO-KBN CHAR(1) '1':入庫 '2':出庫 */
        public String ioKbn = "";
    }

    public static class Pm03 implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** CA03-ITEM-CD CHAR(8) 製造する製品コード */
        public String itemCd = "";
        /** CA03-ORDER-QTY FIXED DEC(9,0) */
        public BigDecimal orderQty = BigDecimal.ZERO;
        /** CA03-DUE-DATE CHAR(10) YYYY-MM-DD */
        public String dueDate = "";
        /** CA03-NG-CNT FIXED BIN(15) */
        public short ngCnt = 0;
        /** CA03-NG-LIST(10) */
        public final NgItem[] ngList = new NgItem[NG_LIST_MAX];

        public Pm03() {
            for (int i = 0; i < NG_LIST_MAX; i++) {
                ngList[i] = new NgItem();
            }
        }
    }

    public static class NgItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** CA03-NG-ITEM-CD CHAR(8) */
        public String itemCd = "";
        /** CA03-NG-NEED-QTY FIXED DEC(9,0) */
        public BigDecimal needQty = BigDecimal.ZERO;
        /** CA03-NG-STOCK-QTY FIXED DEC(9,0) */
        public BigDecimal stockQty = BigDecimal.ZERO;
    }

    public static class Msg implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** CA-MSG-ID CHAR(4) */
        public String msgId = "";
        /** CA-MSG-FLD-POS FIXED BIN(15) エラー項目のカーソル位置（BMS 項目順、0=指定なし） */
        public short msgFldPos = 0;

        public void clear() {
            msgId = "";
            msgFldPos = 0;
        }
    }
}
