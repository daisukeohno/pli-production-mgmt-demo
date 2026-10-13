package jp.co.sanseki.pm.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 共通エラーレスポンス。CA-MSG-ID（messageId）＋ PMMSG 本文 ＋ CA-MSG-FLD-POS 相当のエラー項目（field）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String messageId, String message, String field) {
}
