package jp.co.sanseki.pm.common.message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

/**
 * メッセージカタログ（PMMSG.inc の MSG_TABLE 相当）。
 * classpath:messages/pm-messages.properties（PMMSG 移植）と pm-messages-ext.properties（拡張）を読み込む。
 */
@Component
public class MessageCatalog {

    static final List<String> RESOURCES = List.of("messages/pm-messages.properties", "messages/pm-messages-ext.properties");

    private final Map<String, String> texts;

    public MessageCatalog() {
        Map<String, String> map = new TreeMap<>();
        for (String resource : RESOURCES) {
            map.putAll(load(resource));
        }
        this.texts = Collections.unmodifiableMap(map);
    }

    public Message get(String msgId) {
        String text = texts.get(msgId);
        if (text == null) {
            throw new IllegalArgumentException("未定義のメッセージIDです: " + msgId);
        }
        return new Message(msgId, text);
    }

    public String text(String msgId) {
        return get(msgId).msgText();
    }

    public List<Message> all() {
        return texts.entrySet().stream().map(e -> new Message(e.getKey(), e.getValue())).toList();
    }

    static Map<String, String> load(String resource) {
        Map<String, String> map = new TreeMap<>();
        try (InputStream in = MessageCatalog.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("メッセージ定義が見つかりません: " + resource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                map.put(line.substring(0, eq).trim(), line.substring(eq + 1));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return map;
    }
}
