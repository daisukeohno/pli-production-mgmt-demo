package jp.co.sanseki.pm.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;

/**
 * DB2 TIMESTAMP の文字表現 {@code YYYY-MM-DD-HH.MM.SS.ffffff}（26 桁、CA01-UPD-TMS CHAR(26)）との変換。
 * API の updTms はこの形式の文字列で返し、更新要求でそのまま送り返してもらう。
 */
public final class Db2Timestamp {

    public static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd-HH.mm.ss.SSSSSS").withResolverStyle(ResolverStyle.STRICT);

    private Db2Timestamp() {
    }

    public static String format(LocalDateTime value) {
        return value.format(FORMAT);
    }

    public static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text, FORMAT);
    }

    public static Optional<LocalDateTime> tryParse(String text) {
        if (text == null || text.length() != 26) {
            return Optional.empty();
        }
        try {
            return Optional.of(parse(text));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
