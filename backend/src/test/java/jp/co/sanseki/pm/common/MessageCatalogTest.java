package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MessageCatalogTest {

    private static final Path PMMSG = Path.of("..", "src", "include", "PMMSG.inc");
    private static final Pattern INIT = Pattern.compile("INIT\\('(M\\d{3})','([^']*)'\\)");

    /** PMMSG.inc の INIT 値（CHAR(60) の末尾半角空白のみ除去）。 */
    static Map<String, String> pmmsg() throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        Matcher m = INIT.matcher(Files.readString(PMMSG, StandardCharsets.UTF_8));
        while (m.find()) {
            map.put(m.group(1), Chars.trimPadding(m.group(2)));
        }
        return map;
    }

    @Test
    void pmmsgHas17Messages() throws IOException {
        assertThat(pmmsg().keySet()).containsExactly(
                "M001", "M002", "M003", "M004", "M005", "M006", "M007", "M008", "M009",
                "M010", "M011", "M012", "M013", "M014", "M015", "M016", "M017");
    }

    @Test
    void textsMatchPmmsgExactly() throws IOException {
        Map<String, String> expected = pmmsg();
        Map<String, String> actual = new LinkedHashMap<>();
        Arrays.stream(MessageCatalog.values())
                .filter(m -> expected.containsKey(m.id()))
                .forEach(m -> actual.put(m.id(), m.text()));
        assertThat(actual).containsExactlyEntriesOf(expected);
    }

    @Test
    void onlyM018IsAddedBeyondPmmsg() throws IOException {
        Map<String, String> expected = pmmsg();
        assertThat(Arrays.stream(MessageCatalog.values()).map(MessageCatalog::id).filter(id -> !expected.containsKey(id)))
                .containsExactly("M018");
        assertThat(MessageCatalog.M018.text()).isEqualTo("製造指示番号の連番が上限(9999)を超えました。");
    }

    @Test
    void textsHaveNoPaddingAndFitMsgField() {
        for (MessageCatalog m : MessageCatalog.values()) {
            assertThat(m.text()).as(m.id()).doesNotEndWith(" ").isNotBlank();
            assertThat(m.text().length()).as(m.id() + " は F-MSG 60 桁以内（文字数）").isLessThanOrEqualTo(60);
        }
    }

    @Test
    void successMessages() {
        assertThat(Arrays.stream(MessageCatalog.values()).filter(MessageCatalog::isSuccess))
                .containsExactly(MessageCatalog.M005, MessageCatalog.M006, MessageCatalog.M007,
                        MessageCatalog.M012, MessageCatalog.M013);
        assertThat(MessageCatalog.M002.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(MessageCatalog.M017.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
    }
}
