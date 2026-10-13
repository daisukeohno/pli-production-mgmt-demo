package jp.co.sanseki.pm.controller;

import java.util.Map;

import jp.co.sanseki.pm.common.message.MessageCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** メッセージ定義（MSG_MST）。フロント側の入力チェックでも同じ文言を使うために公開する。 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageCatalog catalog;

    public MessageController(MessageCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public Map<String, String> all() {
        return catalog.all();
    }
}
