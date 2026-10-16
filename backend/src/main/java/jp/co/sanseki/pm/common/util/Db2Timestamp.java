package jp.co.sanseki.pm.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * DB2 TIMESTAMP の文字表現（CHAR(26)：YYYY-MM-DD-HH.MM.SS.ffffff）。
 * COMMAREA の CA01-UPD-TMS と同じ形式で API に受け渡す。
 */
public final class Db2Timestamp {

    public static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd-HH.mm.ss.SSSSSS").withResolverStyle(ResolverStyle.STRICT);

    private Db2Timestamp() {
    }

    public static String format(LocalDateTime value) {
        return FORMAT.format(value.truncatedTo(ChronoUnit.MICROS));
    }

    public static LocalDateTime parse(String value) {
        return LocalDateTime.parse(value, FORMAT);
    }

    public static Optional<LocalDateTime> tryParse(String value) {
        if (value == null || value.length() != 26) {
            return Optional.empty();
        }
        try {
            return Optional.of(parse(value));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
