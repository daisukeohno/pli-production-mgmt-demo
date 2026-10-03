package jp.sanseki.pm.common.commarea;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;

/**
 * EXEC CICS RETURN TRANSID(...) COMMAREA(...) 相当。
 * COMMAREA を端末（HTTP セッション）単位で保持し、次の入力時に受け取る。
 */
@Component
public class CommareaStore {

    static final String ATTR = "PM.COMMAREA";

    /** 受け取った COMMAREA を返す。未保持（EIBCALEN=0 相当）なら CA-FIRST-TIME='Y' の新規エリアを返す。 */
    public Commarea receive(HttpSession session) {
        Object ca = session.getAttribute(ATTR);
        return ca instanceof Commarea c ? c : new Commarea();
    }

    public void returnWith(HttpSession session, Commarea ca) {
        session.setAttribute(ATTR, ca);
    }

    /** RETURN（TRANSID なし）＝疑似会話の終了。 */
    public void end(HttpSession session) {
        session.removeAttribute(ATTR);
    }
}
