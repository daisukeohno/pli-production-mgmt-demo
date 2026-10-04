package jp.sanseki.pm.pm02;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.sanseki.pm.common.commarea.Commarea;
import jp.sanseki.pm.common.msg.PmBusinessException;
import jp.sanseki.pm.common.msg.PmMessage;
import jp.sanseki.pm.common.util.FixedChar;
import jp.sanseki.pm.common.util.Pmutl01;
import jp.sanseki.pm.common.web.Aid;

/**
 * PM02 在庫照会・入出庫登録のメイン処理。
 * 1 回の呼び出しが疑似会話の 1 タスク（RECEIVE MAP → 業務処理 → SEND MAP → RETURN COMMAREA）に相当する。
 * F-IOKBN・F-IOQTY がともに空白なら照会、いずれかに入力があれば入出庫登録として処理する。
 */
@Service
public class Pm02Service {

    public static final String SCRN_ID = "PM02";

    static final String IO_KBN_RECEIPT = "1";
    static final String IO_KBN_ISSUE = "2";

    private final StockRepository repo;
    private final Pmutl01 pmutl01;

    public Pm02Service(StockRepository repo, Pmutl01 pmutl01) {
        this.repo = repo;
        this.pmutl01 = pmutl01;
    }

    /** 処理結果（画面出力＋業務メッセージ）。 */
    public record Result(Pm02Response response, PmMessage msg) {
    }

    /** 初回起動（CA-FIRST-TIME='Y'）。空画面を表示する。 */
    public Result start(Commarea ca) {
        initCommarea(ca);
        return send(ca, emptyFields(""), null, null, false);
    }

    @Transactional
    public Result handle(Commarea ca, Pm02Request in) {
        if (!SCRN_ID.equals(ca.header.scrnId) || Commarea.FIRST_TIME_YES.equals(ca.header.firstTime)) {
            initCommarea(ca);
        }
        ca.msg.clear();
        Aid aid = in.aid() == null ? Aid.ENTER : in.aid();
        Pm02Response.Fields input = inputFields(in);

        return switch (aid) {
            case CLEAR -> start(ca);
            case PF3 -> send(ca, emptyFields(""), null, null, true);
            case ENTER -> enter(ca, input);
            // PM02 で未定義の PF キーは無視して再表示
            default -> send(ca, input, null, null, false);
        };
    }

    // ------------------------------------------------------------------
    // ENTER: 照会 / 入出庫登録
    // ------------------------------------------------------------------
    private Result enter(Commarea ca, Pm02Response.Fields in) {
        Pm02Response.Fields shown = in;
        try {
            requireItemCd(in.itemCd());
            StockItem item = findOrM002(ca, in.itemCd());
            shown = toFields(item, in.ioKbn(), in.ioQty());
            if (in.ioKbn().isEmpty() && in.ioQty().isEmpty()) {
                return inquiry(ca, item);
            }
            validateIoKbn(in.ioKbn());
            BigDecimal qty = parseIoQty(in.ioQty());
            return register(ca, item, in.ioKbn(), qty);
        } catch (StockShortage e) {
            return sendError(ca, toFields(e.item, in.ioKbn(), in.ioQty()), e.error);
        } catch (PmBusinessException e) {
            return sendError(ca, shown, e);
        }
    }

    /** 照会。品目名と現在庫数量を表示し、F-IOKBN へカーソルを置く。 */
    private Result inquiry(Commarea ca, StockItem item) {
        ca.pm02.itemCd = item.itemCd();
        ca.pm02.ioKbn = "";
        return send(ca, toFields(item, "", ""), null, Pm02Field.F_IOKBN, false);
    }

    /**
     * 入出庫登録。STOCK を行ロック付きで再読込してから加減算する。
     * 品目区分 '9'（消耗品）のみマイナス在庫を許容し、それ以外は在庫不足で M011。
     */
    private Result register(Commarea ca, StockItem item, String ioKbn, BigDecimal qty) {
        BigDecimal delta = IO_KBN_RECEIPT.equals(ioKbn) ? qty : qty.negate();
        BigDecimal newQty;
        try {
            newQty = applyDelta(item, delta);
        } catch (DuplicateKeyException e) {
            // STOCK 行の無い品目に他端末が先に INSERT した場合（SQLCODE -803 相当）は再読込して更新する
            newQty = applyDelta(item, delta);
        }
        ca.pm02.itemCd = item.itemCd();
        ca.pm02.ioKbn = ioKbn;
        StockItem updated = new StockItem(item.itemCd(), item.itemName(), item.itemKbn(), newQty);
        return send(ca, toFields(updated, "", ""), PmMessage.M012, null, false);
    }

    private BigDecimal applyDelta(StockItem item, BigDecimal delta) {
        Optional<StockRepository.StockRow> row = repo.findForUpdate(item.itemCd());
        BigDecimal current = row.map(StockRepository.StockRow::stockQty).orElse(BigDecimal.ZERO);
        BigDecimal newQty = current.add(delta);
        StockItem locked = new StockItem(item.itemCd(), item.itemName(), item.itemKbn(), current);
        if (newQty.signum() < 0 && !StockItem.ITEM_KBN_SUPPLY.equals(item.itemKbn())) {
            throw new StockShortage(locked, error(PmMessage.M011, Pm02Field.F_IOQTY));
        }
        if (newQty.compareTo(StockItem.STOCK_QTY_MAX) > 0 || newQty.compareTo(StockItem.STOCK_QTY_MIN) < 0) {
            throw new StockShortage(locked, error(PmMessage.M009, Pm02Field.F_IOQTY));
        }
        if (row.isPresent()) {
            repo.update(item.itemCd(), newQty, nextTimestamp(row.get().updTms()));
        } else {
            repo.insert(item.itemCd(), newQty, pmutl01.currentTimestamp());
        }
        return newQty;
    }

    // ------------------------------------------------------------------
    // 入力チェック
    // ------------------------------------------------------------------
    private static void requireItemCd(String itemCd) {
        if (itemCd.isEmpty() || !FixedChar.fits(itemCd, StockItem.ITEM_CD_LEN)) {
            throw error(PmMessage.M004, Pm02Field.F_ITEMCD);
        }
    }

    private static void validateIoKbn(String ioKbn) {
        if (!IO_KBN_RECEIPT.equals(ioKbn) && !IO_KBN_ISSUE.equals(ioKbn)) {
            throw error(PmMessage.M010, Pm02Field.F_IOKBN);
        }
    }

    /** F-IOQTY（9 桁の符号なし整数）。空白は M004、数字以外は M008、0 や桁あふれは M009。 */
    private static BigDecimal parseIoQty(String ioQty) {
        if (ioQty.isEmpty()) {
            throw error(PmMessage.M004, Pm02Field.F_IOQTY);
        }
        if (!ioQty.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw error(PmMessage.M008, Pm02Field.F_IOQTY);
        }
        if (ioQty.length() > Pm02Field.F_IOQTY.length()) {
            throw error(PmMessage.M009, Pm02Field.F_IOQTY);
        }
        BigDecimal qty = new BigDecimal(ioQty);
        if (qty.signum() == 0) {
            throw error(PmMessage.M009, Pm02Field.F_IOQTY);
        }
        return qty;
    }

    // ------------------------------------------------------------------
    // 共通
    // ------------------------------------------------------------------
    private static void initCommarea(Commarea ca) {
        ca.header.scrnId = SCRN_ID;
        ca.header.funcCd = "";
        ca.header.firstTime = Commarea.FIRST_TIME_NO;
        ca.pm02.itemCd = "";
        ca.pm02.ioKbn = "";
        ca.msg.clear();
    }

    private StockItem findOrM002(Commarea ca, String itemCd) {
        Optional<StockItem> item = repo.findActiveItem(itemCd);
        if (item.isEmpty()) {
            ca.pm02.itemCd = "";
            ca.pm02.ioKbn = "";
            throw error(PmMessage.M002, Pm02Field.F_ITEMCD);
        }
        return item.get();
    }

    /** CURRENT TIMESTAMP。旧値と同一になった場合は 1 マイクロ秒進める。 */
    private LocalDateTime nextTimestamp(LocalDateTime old) {
        LocalDateTime now = pmutl01.currentTimestamp();
        return now.isAfter(old) ? now : old.plusNanos(1_000);
    }

    private static PmBusinessException error(PmMessage msg, Pm02Field field) {
        return new PmBusinessException(msg, field.bmsName());
    }

    private Result sendError(Commarea ca, Pm02Response.Fields shown, PmBusinessException e) {
        return send(ca, shown, e.msg(), Pm02Field.ofBmsName(e.fieldPos()), false);
    }

    private Result send(Commarea ca, Pm02Response.Fields fields, PmMessage msg, Pm02Field cursor, boolean ended) {
        ca.msg.msgId = msg == null ? "" : msg.msgId();
        ca.msg.msgFldPos = cursor == null ? 0 : cursor.order();
        Pm02Response res = new Pm02Response(
                SCRN_ID,
                pmutl01.getSysDate().replace('-', '/'),
                msg == null ? null : msg.msgId(),
                msg == null ? null : msg.msgText(),
                cursor == null ? null : cursor.bmsName(),
                fields,
                ended);
        return new Result(res, msg);
    }

    private static Pm02Response.Fields inputFields(Pm02Request in) {
        return new Pm02Response.Fields(
                FixedChar.rtrim(in.itemCd()),
                "",
                null,
                FixedChar.rtrim(in.ioKbn()),
                FixedChar.rtrim(in.ioQty()));
    }

    private static Pm02Response.Fields emptyFields(String itemCd) {
        return new Pm02Response.Fields(itemCd, "", null, "", "");
    }

    private static Pm02Response.Fields toFields(StockItem item, String ioKbn, String ioQty) {
        return new Pm02Response.Fields(item.itemCd(), item.itemName(), item.stockQtyOrZero(), ioKbn, ioQty);
    }

    /** 在庫チェックで止めた場合に、行ロック後に読んだ在庫数量を画面へ返すための例外。 */
    private static final class StockShortage extends RuntimeException {
        private final StockItem item;
        private final PmBusinessException error;

        StockShortage(StockItem item, PmBusinessException error) {
            super(error.getMessage(), null, false, false);
            this.item = item;
            this.error = error;
        }
    }
}
