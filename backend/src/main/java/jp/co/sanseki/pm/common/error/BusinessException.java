package jp.co.sanseki.pm.common.error;

import org.springframework.http.HttpStatus;

/** 業務エラー。PMMSG のメッセージIDとエラー項目（カーソル位置）を保持する。 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String messageId;
    private final String field;

    public BusinessException(HttpStatus status, String messageId, String field) {
        super(messageId + (field == null ? "" : " (" + field + ")"));
        this.status = status;
        this.messageId = messageId;
        this.field = field;
    }

    public static BusinessException badRequest(String messageId, String field) {
        return new BusinessException(HttpStatus.BAD_REQUEST, messageId, field);
    }

    public static BusinessException notFound(String messageId, String field) {
        return new BusinessException(HttpStatus.NOT_FOUND, messageId, field);
    }

    public static BusinessException conflict(String messageId, String field) {
        return new BusinessException(HttpStatus.CONFLICT, messageId, field);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getField() {
        return field;
    }
}
