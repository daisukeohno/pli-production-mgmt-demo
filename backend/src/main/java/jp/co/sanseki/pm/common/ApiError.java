package jp.co.sanseki.pm.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 全 API 共通のエラー応答 {@code {msgId, message, field}}。
 * 旧 COMMAREA の CA-MSG-ID（msgId）と CA-MSG-FLD-POS（field = エラー項目のリクエスト項目名）の置き換え。
 * 業務メッセージ以外のエラー（形式不正・未定義 API・システムエラー）は msgId=null。
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiError(String msgId, String message, String field) {

    public static ApiError of(MessageCatalog msg, String field) {
        return new ApiError(msg.id(), msg.text(), field);
    }

    public static ApiError system(String message) {
        return new ApiError(null, message, null);
    }
}
