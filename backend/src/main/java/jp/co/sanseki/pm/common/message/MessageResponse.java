package jp.co.sanseki.pm.common.message;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 正常終了時のレスポンス（F-MSG に表示するメッセージ＋処理結果）。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse<T>(String messageId, String message, T data) {
}
