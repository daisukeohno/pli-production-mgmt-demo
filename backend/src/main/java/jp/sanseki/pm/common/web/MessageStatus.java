package jp.sanseki.pm.common.web;

import org.springframework.http.HttpStatus;

import jp.sanseki.pm.common.msg.PmMessage;

/** PMMSG のメッセージ種別と HTTP ステータスの対応。 */
public final class MessageStatus {

    private MessageStatus() {
    }

    public static HttpStatus of(PmMessage msg) {
        if (msg == null) {
            return HttpStatus.OK;
        }
        return switch (msg) {
            case M005, M006, M007, M012, M013 -> HttpStatus.OK;
            case M002, M014 -> HttpStatus.NOT_FOUND;
            case M003, M017 -> HttpStatus.CONFLICT;
            case M011, M015 -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
