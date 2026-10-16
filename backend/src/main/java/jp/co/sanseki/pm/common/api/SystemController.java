package jp.co.sanseki.pm.common.api;

import java.util.List;

import jp.co.sanseki.pm.common.message.Message;
import jp.co.sanseki.pm.common.message.MessageCatalog;
import jp.co.sanseki.pm.common.util.SystemClock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 全画面共通 API（システム日付・メッセージ定義）。 */
@RestController
@RequestMapping("/api")
public class SystemController {

    private final SystemClock clock;
    private final MessageCatalog messages;

    public SystemController(SystemClock clock, MessageCatalog messages) {
        this.clock = clock;
        this.messages = messages;
    }

    /** GET_SYS_DATE。sysDate=YYYY-MM-DD、screenDate=画面1行目表示用 YYYY/MM/DD。 */
    @GetMapping("/system/date")
    public SystemDate date() {
        var today = clock.today();
        return new SystemDate(SystemClock.SYS_DATE.format(today), SystemClock.SCREEN_DATE.format(today));
    }

    @GetMapping("/messages")
    public List<Message> messages() {
        return messages.all();
    }

    public record SystemDate(String sysDate, String screenDate) {
    }
}
