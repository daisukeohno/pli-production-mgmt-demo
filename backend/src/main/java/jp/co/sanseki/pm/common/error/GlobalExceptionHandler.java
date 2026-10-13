package jp.co.sanseki.pm.common.error;

import java.util.ArrayList;
import java.util.List;

import jp.co.sanseki.pm.common.message.MessageCatalog;
import jp.co.sanseki.pm.common.message.MessageIds;
import jp.co.sanseki.pm.common.validation.FieldOrder;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * 全画面共通の例外→{@link ErrorResponse} 変換。
 *
 * <p>Bean Validation の message 属性には PMMSG のメッセージID（例: "M004"）を指定する。
 * 複数項目でエラーがある場合は、BMS の項目順（DTO の宣言順）で最初の項目を返す（CA-MSG-FLD-POS 相当）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageCatalog catalog;

    public GlobalExceptionHandler(MessageCatalog catalog) {
        this.catalog = catalog;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return respond(ex.getStatus(), ex.getMessageId(), ex.getField());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        Class<?> target = ex.getBindingResult().getTarget() == null ? null : ex.getBindingResult().getTarget().getClass();
        return firstFieldError(ex.getBindingResult().getFieldErrors(), target);
    }

    /** パス変数とリクエストボディの両方に制約がある場合（パラメータ順→項目順で先頭を採用）。 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        for (var result : ex.getAllValidationResults()) {
            List<FieldError> fieldErrors = new ArrayList<>();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                if (error instanceof FieldError fe) {
                    fieldErrors.add(fe);
                }
            }
            if (!fieldErrors.isEmpty()) {
                Class<?> target = result.getArgument() == null ? null : result.getArgument().getClass();
                return firstFieldError(fieldErrors, target);
            }
            if (!result.getResolvableErrors().isEmpty()) {
                String field = result.getMethodParameter().getParameterName();
                return respond(HttpStatus.BAD_REQUEST, toMessageId(result.getResolvableErrors().get(0).getDefaultMessage()), field);
            }
        }
        return respond(HttpStatus.BAD_REQUEST, MessageIds.M004, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, MessageIds.M004, null);
    }

    private ResponseEntity<ErrorResponse> firstFieldError(List<FieldError> errors, Class<?> target) {
        FieldError first = errors.stream()
                .min(FieldOrder.comparing(target))
                .orElseThrow();
        return respond(HttpStatus.BAD_REQUEST, toMessageId(first.getDefaultMessage()), first.getField());
    }

    private String toMessageId(String defaultMessage) {
        return defaultMessage != null && catalog.contains(defaultMessage) ? defaultMessage : MessageIds.M004;
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String messageId, String field) {
        return ResponseEntity.status(status).body(new ErrorResponse(messageId, catalog.text(messageId), field));
    }
}
