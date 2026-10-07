package jp.co.sanseki.pm.common;

/** 成功メッセージ（M005 / M006 / M007 / M012 / M013）。更新系 API の応答に含める。 */
public record ApiMessage(String msgId, String message) {

    public static ApiMessage of(MessageCatalog msg) {
        if (!msg.isSuccess()) {
            throw new IllegalArgumentException(msg.id() + " は成功メッセージではありません");
        }
        return new ApiMessage(msg.id(), msg.text());
    }
}
