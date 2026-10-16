package jp.co.sanseki.pm.pm01;

import java.util.List;

import jp.co.sanseki.pm.common.util.Db2Timestamp;

/** PM01 API の入出力。 */
public final class Pm01Dto {

    private Pm01Dto() {
    }

    /**
     * 登録・更新の入力（BMS 入力項目）。
     *
     * @param updTms 更新時のみ。照会で受け取った UPD_TMS（CA01-UPD-TMS）
     */
    public record ItemRequest(String itemCd, String itemName, String itemKbn, String stockUnit, String updTms) {
    }

    /** 照会・登録・更新の結果。msgId/msgText はメッセージ行（照会は空）。 */
    public record ItemResponse(String itemCd, String itemName, String itemKbn, String stockUnit, String updTms,
                               String msgId, String msgText) {

        static ItemResponse of(ItemMst item, String msgId, String msgText) {
            return new ItemResponse(item.itemCd(), item.itemName(), item.itemKbn(), item.stockUnit(),
                    Db2Timestamp.format(item.updTms()), msgId, msgText);
        }
    }

    /** 削除の結果。 */
    public record DeleteResponse(String itemCd, String msgId, String msgText) {
    }

    /** 一覧行（F-LIST01〜F-LIST10：品目コード+品目名+区分）。 */
    public record ListRow(String itemCd, String itemName, String itemKbn) {

        static ListRow of(ItemMst item) {
            return new ListRow(item.itemCd(), item.itemName(), item.itemKbn());
        }
    }

    /**
     * 一覧（10件/頁）。
     *
     * @param cursor 当該頁の先頭品目コード（CA01-TOP-ITEM-CD）
     * @param pageNo 表示頁（CA01-PAGE-NO）
     */
    public record ListResponse(List<ListRow> rows, String cursor, int pageNo, boolean hasPrev, boolean hasNext) {
    }
}
