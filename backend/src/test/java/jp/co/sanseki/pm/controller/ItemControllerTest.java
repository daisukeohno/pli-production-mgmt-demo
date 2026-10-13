package jp.co.sanseki.pm.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jp.co.sanseki.pm.common.db.LegacyDatabaseInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 共通エラーレスポンス（messageId / message / field）と入力チェックの優先順。 */
@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    LegacyDatabaseInitializer initializer;

    @BeforeEach
    void reset() {
        initializer.reset();
    }

    @Test
    void messages_areServedFromMsgMst() throws Exception {
        mvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.M001").value("品目コードが不正です。チェックデジットを確認してください。"))
                .andExpect(jsonPath("$.M017").value("他の端末で更新されています。再照会してください。"))
                .andExpect(jsonPath("$.length()").value(17));
    }

    @Test
    void create_requiredFieldsFollowBmsOrder() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemCd\":\"\",\"itemName\":\"\",\"itemKbn\":\"\",\"stockUnit\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messageId").value("M004"))
                .andExpect(jsonPath("$.message").value("必須項目が未入力です。"))
                .andExpect(jsonPath("$.field").value("itemCd"));
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemCd\":\"20000110\",\"itemName\":\"部品\",\"itemKbn\":\"3\",\"stockUnit\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messageId").value("M004"))
                .andExpect(jsonPath("$.field").value("itemKbn"));
    }

    @Test
    void create_tooLongNameIsRejected() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemCd\":\"20000110\",\"itemName\":\"123456789012345678901\",\"itemKbn\":\"2\",\"stockUnit\":\"個\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("itemName"));
    }

    @Test
    void update_checkDigitOnPathComesFirst() throws Exception {
        mvc.perform(put("/api/items/20000111").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"\",\"itemKbn\":\"2\",\"stockUnit\":\"個\",\"updTms\":\"2026-09-01-09.00.00.000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messageId").value("M001"))
                .andExpect(jsonPath("$.field").value("itemCd"));
        mvc.perform(put("/api/items/20000059").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"\",\"itemKbn\":\"2\",\"stockUnit\":\"個\",\"updTms\":\"2026-09-01-09.00.00.000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messageId").value("M004"))
                .andExpect(jsonPath("$.field").value("itemName"));
    }

    @Test
    void update_withoutHeldUpdTmsIsM017() throws Exception {
        mvc.perform(put("/api/items/20000059").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"x\",\"itemKbn\":\"2\",\"stockUnit\":\"個\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messageId").value("M017"));
        mvc.perform(delete("/api/items/20000059"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messageId").value("M017"));
    }

    @Test
    void update_unknownItemIsM002() throws Exception {
        mvc.perform(put("/api/items/99999995").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"x\",\"itemKbn\":\"2\",\"stockUnit\":\"個\",\"updTms\":\"2026-09-01-09.00.00.000000\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messageId").value("M002"));
    }

    @Test
    void create_logicallyDeletedCodeCannotBeReused() throws Exception {
        mvc.perform(delete("/api/items/20000073").param("updTms", "2026-09-01-09.00.00.000000"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemCd\":\"20000073\",\"itemName\":\"再登録\",\"itemKbn\":\"2\",\"stockUnit\":\"個\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messageId").value("M003"));
    }
}
