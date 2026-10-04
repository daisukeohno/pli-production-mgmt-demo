package jp.sanseki.pm.pm02;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import jp.sanseki.pm.common.commarea.Commarea;
import jp.sanseki.pm.common.commarea.CommareaStore;
import jp.sanseki.pm.common.web.MessageStatus;

/**
 * PM02 在庫照会・入出庫登録 API。
 * GET  /api/pm02 : トランザクション開始（初回表示）
 * POST /api/pm02 : 注意キー（ENTER/CLEAR/PF3）＋入力項目
 */
@RestController
@RequestMapping("/api/pm02")
public class Pm02Controller {

    private final Pm02Service service;
    private final CommareaStore store;

    public Pm02Controller(Pm02Service service, CommareaStore store) {
        this.service = service;
        this.store = store;
    }

    @GetMapping
    public ResponseEntity<Pm02Response> start(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Commarea ca = new Commarea();
            Pm02Service.Result r = service.start(ca);
            store.returnWith(session, ca);
            return ResponseEntity.ok(r.response());
        }
    }

    @PostMapping
    public ResponseEntity<Pm02Response> input(@RequestBody Pm02Request req, HttpSession session) {
        // 1 端末で同時に実行されるタスクは 1 つ（CICS と同じく端末単位で直列化）
        synchronized (WebUtils.getSessionMutex(session)) {
            Commarea ca = store.receive(session);
            Pm02Service.Result r = service.handle(ca, req);
            if (r.response().ended()) {
                store.end(session);
            } else {
                store.returnWith(session, ca);
            }
            HttpStatus status = MessageStatus.of(r.msg());
            return ResponseEntity.status(status).body(r.response());
        }
    }
}
