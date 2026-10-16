package jp.co.sanseki.pm.common.error;

import org.springframework.http.HttpStatus;

/** 業務エラー。メッセージ行に msgId の文言を表示し、field へカーソルを位置付ける。 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String msgId;
    private final ScreenField field;

    public BusinessException(HttpStatus status, String msgId, ScreenField field) {
        super(msgId);
        this.status = status;
        this.msgId = msgId;
        this.field = field;
    }

    public static BusinessException badRequest(String msgId, ScreenField field) {
        return new BusinessException(HttpStatus.BAD_REQUEST, msgId, field);
    }

    public static BusinessException notFound(String msgId, ScreenField field) {
        return new BusinessException(HttpStatus.NOT_FOUND, msgId, field);
    }

    public static BusinessException conflict(String msgId, ScreenField field) {
        return new BusinessException(HttpStatus.CONFLICT, msgId, field);
    }

    public HttpStatus status() {
        return status;
    }

    public String msgId() {
        return msgId;
    }

    public ScreenField field() {
        return field;
    }
}
