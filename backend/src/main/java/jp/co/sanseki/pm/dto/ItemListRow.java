package jp.co.sanseki.pm.dto;

import jp.co.sanseki.pm.common.util.FixedChar;
import jp.co.sanseki.pm.domain.ItemMst;

/** 一覧1行（F-LISTnn：品目コード＋品目名＋区分）。 */
public record ItemListRow(String itemCd, String itemName, String itemKbn) {

    public static ItemListRow from(ItemMst item) {
        return new ItemListRow(
                FixedChar.rtrim(item.getItemCd()),
                FixedChar.rtrim(item.getItemName()),
                FixedChar.rtrim(item.getItemKbn()));
    }
}
