package jp.co.sanseki.pm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jp.co.sanseki.pm.common.validation.OneOf;
import jp.co.sanseki.pm.common.validation.ValidItemCd;

/** 登録（機能コード2）。項目の宣言順は BMS の項目順（エラー時のカーソル位置の優先順）。 */
public record ItemCreateRequest(
        @NotBlank(message = "M004") @ValidItemCd String itemCd,
        @NotBlank(message = "M004") @Size(max = 20, message = "M004") String itemName,
        @NotBlank(message = "M004") @OneOf(value = {"1", "2", "9"}, message = "M004") String itemKbn,
        @NotBlank(message = "M004") @Size(max = 4, message = "M004") String stockUnit) {
}
