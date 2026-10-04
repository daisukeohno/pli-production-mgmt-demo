package jp.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DdlConversionTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void initialDataLoaded() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SANSEKI.ITEM_MST", Integer.class)).isEqualTo(15);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SANSEKI.BOM", Integer.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SANSEKI.STOCK", Integer.class)).isEqualTo(15);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SANSEKI.WORK_ORDER", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT SEQ_NO FROM SANSEKI.SEQ_CTL", Integer.class)).isEqualTo(2);
    }

    @Test
    void charColumnsArePaddedAndCompareBlankInsensitive() {
        // CHAR(20) は空白パディングで格納され、DB2 同様に末尾空白を無視して比較される
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SANSEKI.ITEM_MST WHERE ITEM_NAME = 'オイルシール'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ITEM_NAME FROM SANSEKI.ITEM_MST WHERE ITEM_CD = '20000073'",
                String.class)).startsWith("オイルシール");
    }

    @Test
    void foreignKeysAreEnforced() {
        Integer fks = jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS"
                + " WHERE CONSTRAINT_SCHEMA = 'SANSEKI' AND CONSTRAINT_TYPE = 'FOREIGN KEY'", Integer.class);
        assertThat(fks).isEqualTo(4);
    }
}
