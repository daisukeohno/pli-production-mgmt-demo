package jp.co.sanseki.pm.common.message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jp.co.sanseki.pm.common.db.LegacyDatabaseInitializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** メッセージID→本文。サーバー側のテーブル SANSEKI.MSG_MST（PMMSG.inc から投入）を正とする。 */
@Component
public class MessageCatalog {

    private final JdbcTemplate jdbc;
    private volatile Map<String, String> messages;

    public MessageCatalog(JdbcTemplate jdbc, LegacyDatabaseInitializer initializer) {
        this.jdbc = jdbc;
        initializer.addResetListener(this::reload);
        reload();
    }

    public void reload() {
        Map<String, String> loaded = new LinkedHashMap<>();
        jdbc.query("SELECT MSG_ID, MSG_TEXT FROM SANSEKI.MSG_MST ORDER BY MSG_ID",
                rs -> {
                    loaded.put(rs.getString("MSG_ID").trim(), rs.getString("MSG_TEXT").trim());
                });
        this.messages = Collections.unmodifiableMap(loaded);
    }

    public String text(String messageId) {
        String text = messages.get(messageId);
        if (text == null) {
            throw new IllegalArgumentException("未定義のメッセージIDです: " + messageId);
        }
        return text;
    }

    public boolean contains(String messageId) {
        return messages.containsKey(messageId);
    }

    public Map<String, String> all() {
        return messages;
    }
}
