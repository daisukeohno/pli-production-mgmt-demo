package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.sanseki.pm.common.message.Message;
import jp.co.sanseki.pm.common.message.MessageCatalog;
import org.junit.jupiter.api.Test;

/** pm-messages.properties が PMMSG.inc の MSG_TABLE と一字一句一致することを照合する。 */
class MessageCatalogTest {

    private static final Pattern INIT = Pattern.compile("INIT\\('(M\\d{3})','([^']*)'\\);");

    @Test
    void messagesMatchPmmsgInc() throws IOException {
        Map<String, String> legacy = new LinkedHashMap<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("legacy/include/PMMSG.inc")) {
            Matcher m = INIT.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            while (m.find()) {
                // MSG_TEXT は CHAR(60) の空白埋め。移行先は末尾空白を除いた文言を持つ
                legacy.put(m.group(1), m.group(2).stripTrailing());
            }
        }
        assertThat(legacy).hasSize(17);

        MessageCatalog catalog = new MessageCatalog();
        legacy.forEach((id, text) -> assertThat(catalog.text(id)).as(id).isEqualTo(text));
        assertThat(catalog.all().stream().map(Message::msgId).filter(id -> id.startsWith("M"))).hasSize(17);
    }

    @Test
    void extensionMessagesAreSeparatedFromLegacyIds() {
        MessageCatalog catalog = new MessageCatalog();
        assertThat(catalog.all().stream().map(Message::msgId).filter(id -> !id.startsWith("M")))
                .containsExactly("X001", "X002");
    }
}
