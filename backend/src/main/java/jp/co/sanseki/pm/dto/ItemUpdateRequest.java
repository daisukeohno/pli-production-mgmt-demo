package jp.co.sanseki.pm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jp.co.sanseki.pm.common.validation.OneOf;

/** 更新（機能コード3）。updTms は照会時に受け取った値（未保持・不一致は M017）。 */
public record ItemUpdateRequest(
        @NotBlank(message = "M004") @Size(max = 20, message = "M004") String itemName,
        @NotBlank(message = "M004") @OneOf(value = {"1", "2", "9"}, message = "M004") String itemKbn,
        @NotBlank(message = "M004") @Size(max = 4, message = "M004") String stockUnit,
        String updTms) {
}
