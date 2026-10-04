package jp.sanseki.pm.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** DB2 TIMESTAMP の文字表現 'YYYY-MM-DD-HH.MM.SS.NNNNNN'（CHAR(26)）との相互変換。 */
public final class Db2Timestamp {

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH.mm.ss.SSSSSS");
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd");

    private Db2Timestamp() {
    }

    public static String format(LocalDateTime ts) {
        return ts == null ? "" : FORMAT.format(ts);
    }

    public static LocalDateTime parse(String s) {
        return LocalDateTime.parse(FixedChar.rtrim(s), FORMAT);
    }

    public static LocalDate parseDate(String s) {
        return LocalDate.parse(FixedChar.rtrim(s), DATE_FORMAT);
    }
}
