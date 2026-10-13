package jp.co.sanseki.pm.common.lock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** DB2 TIMESTAMP の文字表現（CHAR(26)：YYYY-MM-DD-HH.MM.SS.NNNNNN）との相互変換。 */
public final class Db2Timestamp {

    public static final Pattern PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{6}");
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH.mm.ss.SSSSSS");

    private Db2Timestamp() {
    }

    public static String format(LocalDateTime value) {
        return value == null ? null : FORMAT.format(value);
    }

    /** 不正な形式・空の場合は null。 */
    public static LocalDateTime parseOrNull(String value) {
        if (value == null || !PATTERN.matcher(value).matches()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
