package jp.sanseki.pm.pm03;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.sanseki.pm.common.commarea.Commarea;
import jp.sanseki.pm.common.msg.PmBusinessException;
import jp.sanseki.pm.common.msg.PmMessage;
import jp.sanseki.pm.common.util.FixedChar;
import jp.sanseki.pm.common.util.Pmutl01;
import jp.sanseki.pm.common.web.Aid;
import jp.sanseki.pm.pm01.ItemMst;
import jp.sanseki.pm.pm01.ItemMstRepository;
import jp.sanseki.pm.pm03.Pm03Repository.Bom;

/**
 * PM03 製造指示登録のメイン処理。
 * 1 回の呼び出しが疑似会話の 1 タスク（RECEIVE MAP → 業務処理 → SEND MAP → RETURN COMMAREA）に相当する。
 * PM03 には F-FUNC が無く、ENTER が製造指示登録、PF4 が在庫不足部品一覧の表示に対応する。
 */
@Service
public class Pm03Service {

    public static final String SCRN_ID = "PM03";
    public static final int ORDER_QTY_LEN = 9;
    public static final int DUE_DATE_LEN = 10;

    private static final DateTimeFormatter DUE_DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private final Pm03Repository repo;
    private final ItemMstRepository itemMst;
    private final Pmutl01 pmutl01;

    public Pm03Service(Pm03Repository repo, ItemMstRepository itemMst, Pmutl01 pmutl01) {
        this.repo = repo;
        this.itemMst = itemMst;
        this.pmutl01 = pmutl01;
    }

    /** 処理結果（画面出力＋業務メッセージ）。 */
    public record Result(Pm03Response response, PmMessage msg) {
    }

    /** 初回起動（CA-FIRST-TIME='Y'）。空画面を表示する。 */
    @Transactional(readOnly = true)
    public Result start(Commarea ca) {
        initCommarea(ca);
        return send(ca, emptyFields(), null, null, false, false);
    }

    @Transactional
    public Result handle(Commarea ca, Pm03Request in) {
        if (!SCRN_ID.equals(ca.header.scrnId) || Commarea.FIRST_TIME_YES.equals(ca.header.firstTime)) {
            initCommarea(ca);
        }
        ca.msg.clear();
        Aid aid = in.aid() == null ? Aid.ENTER : in.aid();
        Pm03Response.Fields input = inputFields(in);

        return switch (aid) {
            case CLEAR -> start(ca);
            case PF3 -> send(ca, emptyFields(), null, null, true, false);
            case PF4 ->
                // F-NGLIST01〜10 へ CA03-NG-LIST を送信するのは PF4 のみ。
                // 画面項目は CA03 に退避した直前の登録入力をそのまま返す（入力消去しない）
                send(ca, caFields(ca), null, null, false, true);
            case ENTER -> enter(ca, input);
            // PM03 で未定義の PF キーは無視して再表示
            default -> send(ca, input, null, null, false, false);
        };
    }

    // ------------------------------------------------------------------
    // ENTER: 製造指示登録
    // ------------------------------------------------------------------
    private Result enter(Commarea ca, Pm03Response.Fields in) {
        try {
            return register(ca, in);
        } catch (PmBusinessException e) {
            return sendError(ca, in, e);
        }
    }

    private Result register(Commarea ca, Pm03Response.Fields f) {
        // 新しい登録操作では前回の不足一覧を消す
        clearNgList(ca);
        saveInput(ca, f);

        validateInput(f);
        ItemMst product = itemMst.findActive(f.itemCd())
                .orElseThrow(() -> error(PmMessage.M002, Pm03Field.F_ITEMCD));

        List<Bom> bom = repo.findBom(f.itemCd());
        if (bom.isEmpty()) {
            throw error(PmMessage.M014, Pm03Field.F_ITEMCD);
        }

        BigDecimal orderQty = new BigDecimal(f.orderQty());
        List<Pm03Response.NgRow> ngRows = shortageRows(orderQty, bom);
        if (!ngRows.isEmpty()) {
            setNgList(ca, ngRows);
            throw error(PmMessage.M015, null);
        }

        // 採番は全チェック通過後（不足時に番号を消費しない）
        String orderNo = repo.nextWorkOrderNo(currentYearYy());
        repo.insertWorkOrder(orderNo, f.itemCd(), orderQty,
                Db2LocalDate.parse(f.dueDate()), pmutl01.currentTimestamp());

        Pm03Response.Fields out = new Pm03Response.Fields(
                f.itemCd(), product.itemName(), f.orderQty(), f.dueDate(), orderNo);
        return send(ca, out, PmMessage.M013, null, false, false);
    }

    /** 子部品必要数 = 指示数量×員数÷歩留まり率（切り上げ。design.md §2 ルール6）。 */
    private List<Pm03Response.NgRow> shortageRows(BigDecimal orderQty, List<Bom> bom) {
        List<Pm03Response.NgRow> ng = new ArrayList<>();
        for (Bom b : bom) {
            BigDecimal needQty = needQty(orderQty, b);
            BigDecimal stockQty = repo.stockQty(b.childItemCd()).orElse(BigDecimal.ZERO);
            if (stockQty.compareTo(needQty) < 0) {
                ng.add(new Pm03Response.NgRow(b.childItemCd(), needQty, stockQty));
            }
        }
        return ng;
    }

    /** CEIL(orderQty × qtyPer ÷ (yieldRate/100))。歩留まり率は % 値（例 95.00）。 */
    static BigDecimal needQty(BigDecimal orderQty, Bom b) {
        return orderQty.multiply(b.qtyPer()).multiply(BigDecimal.valueOf(100))
                .divide(b.yieldRate(), 10, RoundingMode.CEILING)
                .setScale(0, RoundingMode.CEILING);
    }

    // ------------------------------------------------------------------
    // 入力チェック（BMS 項目順）
    // ------------------------------------------------------------------
    private static void validateInput(Pm03Response.Fields f) {
        if (f.itemCd().isEmpty() || !FixedChar.fits(f.itemCd(), ItemMst.ITEM_CD_LEN)) {
            throw error(PmMessage.M004, Pm03Field.F_ITEMCD);
        }
        if (f.orderQty().isEmpty()) {
            throw error(PmMessage.M004, Pm03Field.F_ORDERQTY);
        }
        if (!f.orderQty().chars().allMatch(c -> c >= '0' && c <= '9')
                || f.orderQty().length() > ORDER_QTY_LEN || new BigDecimal(f.orderQty()).signum() <= 0) {
            throw error(PmMessage.M008, Pm03Field.F_ORDERQTY);
        }
        if (f.dueDate().isEmpty()) {
            throw error(PmMessage.M004, Pm03Field.F_DUEDATE);
        }
        try {
            Db2LocalDate.parse(f.dueDate());
        } catch (DateTimeParseException e) {
            throw error(PmMessage.M016, Pm03Field.F_DUEDATE);
        }
    }

    // ------------------------------------------------------------------
    // COMMAREA（CA-PM03 領域）
    // ------------------------------------------------------------------
    private static void initCommarea(Commarea ca) {
        ca.header.scrnId = SCRN_ID;
        ca.header.funcCd = "";
        ca.header.firstTime = Commarea.FIRST_TIME_NO;
        ca.pm03.itemCd = "";
        ca.pm03.orderQty = BigDecimal.ZERO;
        ca.pm03.dueDate = "";
        clearNgList(ca);
        ca.msg.clear();
    }

    private static void saveInput(Commarea ca, Pm03Response.Fields f) {
        ca.pm03.itemCd = f.itemCd();
        ca.pm03.orderQty = f.orderQty().isEmpty() || !f.orderQty().chars().allMatch(Character::isDigit)
                ? BigDecimal.ZERO : new BigDecimal(f.orderQty());
        ca.pm03.dueDate = f.dueDate();
    }

    private static void setNgList(Commarea ca, List<Pm03Response.NgRow> ngRows) {
        clearNgList(ca);
        int n = Math.min(ngRows.size(), Commarea.NG_LIST_MAX);
        ca.pm03.ngCnt = (short) n;
        for (int i = 0; i < n; i++) {
            ca.pm03.ngList[i].itemCd = ngRows.get(i).itemCd();
            ca.pm03.ngList[i].needQty = ngRows.get(i).needQty();
            ca.pm03.ngList[i].stockQty = ngRows.get(i).stockQty();
        }
    }

    private static void clearNgList(Commarea ca) {
        ca.pm03.ngCnt = 0;
        for (Commarea.NgItem ng : ca.pm03.ngList) {
            ng.itemCd = "";
            ng.needQty = BigDecimal.ZERO;
            ng.stockQty = BigDecimal.ZERO;
        }
    }

    private static List<Pm03Response.NgRow> ngListOf(Commarea ca) {
        List<Pm03Response.NgRow> rows = new ArrayList<>();
        for (int i = 0; i < ca.pm03.ngCnt; i++) {
            Commarea.NgItem ng = ca.pm03.ngList[i];
            rows.add(new Pm03Response.NgRow(FixedChar.rtrim(ng.itemCd), ng.needQty, ng.stockQty));
        }
        return rows;
    }

    /** 西暦下2桁（GET_SYS_DATE の先頭2桁は 20 固定のため末尾2桁を切り出す）。 */
    private String currentYearYy() {
        String sysDate = pmutl01.getSysDate(); // YYYY-MM-DD
        return sysDate.substring(2, 4);
    }

    // ------------------------------------------------------------------
    // 共通
    // ------------------------------------------------------------------
    private static PmBusinessException error(PmMessage msg, Pm03Field field) {
        return new PmBusinessException(msg, field == null ? null : field.bmsName());
    }

    private Result sendError(Commarea ca, Pm03Response.Fields in, PmBusinessException e) {
        Pm03Field field = null;
        for (Pm03Field f : Pm03Field.values()) {
            if (f.bmsName().equals(e.fieldPos())) {
                field = f;
            }
        }
        return send(ca, in, e.msg(), field, false, false);
    }

    /** sendNg = true のときだけ F-NGLIST01〜10 へ CA03-NG-LIST を送る（PF4）。 */
    private Result send(Commarea ca, Pm03Response.Fields fields, PmMessage msg, Pm03Field cursor,
            boolean ended, boolean sendNg) {
        ca.msg.msgId = msg == null ? "" : msg.msgId();
        ca.msg.msgFldPos = cursor == null ? 0 : cursor.order();
        Pm03Response res = new Pm03Response(
                SCRN_ID,
                pmutl01.getSysDate().replace('-', '/'),
                msg == null ? null : msg.msgId(),
                msg == null ? null : msg.msgText(),
                cursor == null ? null : cursor.bmsName(),
                fields,
                sendNg && !ended ? ngListOf(ca) : List.of(),
                ended);
        return new Result(res, msg);
    }

    private static Pm03Response.Fields inputFields(Pm03Request in) {
        return new Pm03Response.Fields(
                FixedChar.rtrim(in.itemCd()),
                "",
                FixedChar.rtrim(in.orderQty()),
                FixedChar.rtrim(in.dueDate()),
                "");
    }

    private static Pm03Response.Fields emptyFields() {
        return new Pm03Response.Fields("", "", "", "", "");
    }

    /** CA03 に退避した直前の登録入力を画面項目へ戻す。F-ITEMNM / F-ORDERNO は CA03 に無いため空白。 */
    private static Pm03Response.Fields caFields(Commarea ca) {
        return new Pm03Response.Fields(
                FixedChar.rtrim(ca.pm03.itemCd), "",
                ca.pm03.orderQty.signum() > 0 ? ca.pm03.orderQty.toPlainString() : "",
                FixedChar.rtrim(ca.pm03.dueDate), "");
    }

    /** 'YYYY-MM-DD' 厳密パース（2026-02-30 や 2026/12/01 はエラー）。 */
    private static final class Db2LocalDate {
        static LocalDate parse(String s) {
            if (s.length() != DUE_DATE_LEN) {
                throw new DateTimeParseException("形式不正", s, 0);
            }
            return LocalDate.parse(s, DUE_DATE_FORMAT);
        }
    }
}
