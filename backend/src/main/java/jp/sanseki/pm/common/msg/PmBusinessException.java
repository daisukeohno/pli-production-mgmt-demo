package jp.sanseki.pm.common.msg;

/** 業務エラー（PMMSG のメッセージIDとカーソル位置を伴う）。 */
public class PmBusinessException extends RuntimeException {

    private final PmMessage msg;
    private final String fieldPos;

    public PmBusinessException(PmMessage msg, String fieldPos) {
        super(msg.msgId() + " " + msg.msgText());
        this.msg = msg;
        this.fieldPos = fieldPos;
    }

    public PmMessage msg() {
        return msg;
    }

    public String fieldPos() {
        return fieldPos;
    }
}
