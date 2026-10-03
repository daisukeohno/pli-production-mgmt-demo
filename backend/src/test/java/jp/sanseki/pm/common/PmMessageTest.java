package jp.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import jp.sanseki.pm.common.msg.PmMessage;

class PmMessageTest {

    @Test
    void messagesMatchPmmsgInclude() throws IOException {
        String inc = Files.readString(Path.of("../src/include/PMMSG.inc"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("INIT\\('(M\\d{3})','(.*?)'\\);").matcher(inc);
        int n = 0;
        while (m.find()) {
            PmMessage msg = PmMessage.of(m.group(1));
            assertThat(msg.initLiteral()).isEqualTo(m.group(2));
            assertThat(msg.msgTextChar60()).hasSize(60);
            assertThat(msg.msgText()).isEqualTo(m.group(2).stripTrailing());
            n++;
        }
        assertThat(n).isEqualTo(17);
        assertThat(PmMessage.values()).hasSize(17);
    }
}
