package jp.co.sanseki.pm.common.error;

/**
 * エラーレスポンス（全画面共通）。
 *
 * @param msgId    メッセージID（CA-MSG-ID）
 * @param msgText  メッセージ文言（F-MSG）
 * @param field    エラー項目名（null の場合は項目指定なし）
 * @param fieldPos エラー項目の BMS 項目順位置（CA-MSG-FLD-POS、項目指定なしは 0）
 */
public record ErrorResponse(String msgId, String msgText, String field, int fieldPos) {
}
