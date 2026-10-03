package jp.sanseki.pm.pm01;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.sanseki.pm.common.commarea.Commarea;
import jp.sanseki.pm.common.commarea.CommareaStore;
import jp.sanseki.pm.common.web.MessageStatus;

/**
 * PM01 品目マスタ保守 API。
 * GET  /api/pm01 : トランザクション開始（初回表示）
 * POST /api/pm01 : 注意キー（ENTER/CLEAR/PF3/PF7/PF8）＋入力項目
 */
@RestController
@RequestMapping("/api/pm01")
public class Pm01Controller {

    private final Pm01Service service;
    private final CommareaStore store;

    public Pm01Controller(Pm01Service service, CommareaStore store) {
        this.service = service;
        this.store = store;
    }

    @GetMapping
    public ResponseEntity<Pm01Response> start(HttpSession session) {
        Commarea ca = new Commarea();
        Pm01Service.Result r = service.start(ca);
        store.returnWith(session, ca);
        return ResponseEntity.ok(r.response());
    }

    @PostMapping
    public ResponseEntity<Pm01Response> input(@RequestBody Pm01Request req, HttpSession session) {
        Commarea ca = store.receive(session);
        Pm01Service.Result r = service.handle(ca, req);
        if (r.response().ended()) {
            store.end(session);
        } else {
            store.returnWith(session, ca);
        }
        HttpStatus status = MessageStatus.of(r.msg());
        return ResponseEntity.status(status).body(r.response());
    }
}
