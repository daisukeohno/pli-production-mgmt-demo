package jp.co.sanseki.pm.common;

/** 業務エラー。GlobalExceptionHandler が {@link ApiError} に変換する。 */
public class BusinessException extends RuntimeException {

    private final MessageCatalog msg;
    private final String field;

    public BusinessException(MessageCatalog msg) {
        this(msg, null);
    }

    public BusinessException(MessageCatalog msg, String field) {
        super(msg.id() + " " + msg.text());
        if (msg.isSuccess()) {
            throw new IllegalArgumentException(msg.id() + " は成功メッセージです");
        }
        this.msg = msg;
        this.field = field;
    }

    public MessageCatalog msg() {
        return msg;
    }

    public String field() {
        return field;
    }
}
