package jp.co.sanseki.pm.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import jp.co.sanseki.pm.common.db.LegacyDatabaseInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LegacyDatabaseInitializerTest {

    @Test
    void ddl_keepsColumnsAndRemovesOnlyDb2SpecificClauses() throws Exception {
        String ddl = new ClassPathResource("legacy/db2/ddl/01_item_mst.sql").getContentAsString(StandardCharsets.UTF_8);
        List<String> stmts = LegacyDatabaseInitializer.toPortableStatements(ddl);
        assertThat(stmts).hasSize(2);
        assertThat(stmts.get(0))
                .startsWith("CREATE TABLE SANSEKI.ITEM_MST")
                .contains("ITEM_CD      CHAR(8)        NOT NULL")
                .contains("DEL_FLG      CHAR(1)        NOT NULL DEFAULT '0'")
                .contains("UPD_TMS      TIMESTAMP      NOT NULL")
                .contains("PRIMARY KEY (ITEM_CD)")
                .doesNotContain(" IN SANSEKI.TS001");
        assertThat(stmts.get(1)).contains("CREATE INDEX SANSEKI.IX_ITEM_KBN");
    }

    @Test
    void ddl_namedForeignKeyIsRewritten() throws Exception {
        String ddl = new ClassPathResource("legacy/db2/ddl/02_bom.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(LegacyDatabaseInitializer.toPortableStatements(ddl))
                .anySatisfy(s -> assertThat(s).contains("ADD CONSTRAINT FK_BOM_PARENT FOREIGN KEY (PARENT_ITEM_CD)"));
    }

    @Test
    void pmmsg_all17MessagesAreParsed() throws Exception {
        String inc = new ClassPathResource("legacy/include/PMMSG.inc").getContentAsString(StandardCharsets.UTF_8);
        List<String[]> msgs = LegacyDatabaseInitializer.parseMessages(inc);
        assertThat(msgs).hasSize(17);
        assertThat(msgs.get(0)).containsExactly("M001", "品目コードが不正です。チェックデジットを確認してください。");
        assertThat(msgs.get(16)).containsExactly("M017", "他の端末で更新されています。再照会してください。");
        assertThat(msgs).allSatisfy(m -> assertThat(m[1].length()).isLessThanOrEqualTo(60));
    }
}
