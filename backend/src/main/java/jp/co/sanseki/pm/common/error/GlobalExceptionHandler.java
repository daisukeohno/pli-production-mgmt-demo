package jp.co.sanseki.pm.common.error;

import jp.co.sanseki.pm.common.message.Message;
import jp.co.sanseki.pm.common.message.MessageCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageCatalog messages;

    public GlobalExceptionHandler(MessageCatalog messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> business(BusinessException e) {
        Message m = messages.get(e.msgId());
        ScreenField f = e.field();
        return ResponseEntity.status(e.status())
                .body(new ErrorResponse(m.msgId(), m.msgText(), f == null ? null : f.fieldName(), f == null ? 0 : f.position()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> unreadable(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(null, "リクエストの形式が不正です。", null, 0));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        log.error("想定外のエラー", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(null, "システムエラーが発生しました。", null, 0));
    }
}
