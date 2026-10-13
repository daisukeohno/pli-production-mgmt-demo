package jp.co.sanseki.pm.dto;

import jp.co.sanseki.pm.common.lock.Db2Timestamp;
import jp.co.sanseki.pm.common.util.FixedChar;
import jp.co.sanseki.pm.domain.ItemMst;

/** 品目照会結果。updTms は更新・削除時に送り返す（CA01-UPD-TMS 相当、YYYY-MM-DD-HH.MM.SS.NNNNNN）。 */
public record ItemResponse(String itemCd, String itemName, String itemKbn, String stockUnit, String updTms) {

    public static ItemResponse from(ItemMst item) {
        return new ItemResponse(
                FixedChar.rtrim(item.getItemCd()),
                FixedChar.rtrim(item.getItemName()),
                FixedChar.rtrim(item.getItemKbn()),
                FixedChar.rtrim(item.getStockUnit()),
                Db2Timestamp.format(item.getUpdTms()));
    }
}
