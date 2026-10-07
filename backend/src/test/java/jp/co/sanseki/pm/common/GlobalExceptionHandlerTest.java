package jp.co.sanseki.pm.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ProbeController.class)
@Import({GlobalExceptionHandlerTest.ProbeController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @RestController
    static class ProbeController {

        @GetMapping("/api/_probe/{msgId}")
        Object fail(@PathVariable String msgId) {
            throw new BusinessException(MessageCatalog.valueOf(msgId), "itemCd");
        }

        @GetMapping("/api/_probe/nofield")
        Object noField() {
            throw new BusinessException(MessageCatalog.M017);
        }

        @PostMapping("/api/_probe/body")
        Object body(@RequestBody Map<String, Object> body) {
            return ApiMessage.of(MessageCatalog.M005);
        }

        @GetMapping("/api/_probe/boom")
        Object boom() {
            throw new IllegalStateException("boom");
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void businessErrorBody() throws Exception {
        mvc.perform(get("/api/_probe/M002"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"msgId":"M002","message":"該当する品目は登録されていません。","field":"itemCd"}
                        """, true));
        mvc.perform(get("/api/_probe/M001")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msgId").value("M001"));
        mvc.perform(get("/api/_probe/M015")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void fieldIsNullWhenNotSpecified() throws Exception {
        mvc.perform(get("/api/_probe/nofield"))
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {"msgId":"M017","message":"他の端末で更新されています。再照会してください。","field":null}
                        """, true));
    }

    @Test
    void successMessageBody() throws Exception {
        mvc.perform(post("/api/_probe/body").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"msgId":"M005","message":"登録しました。"}
                        """, true));
    }

    @Test
    void malformedJsonHasNullMsgId() throws Exception {
        mvc.perform(post("/api/_probe/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"msgId":null,"message":"リクエストの形式が不正です。","field":null}
                        """, true));
    }

    @Test
    void unknownPathAndUnexpectedErrorUseSameShape() throws Exception {
        mvc.perform(get("/api/no-such-api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.msgId").doesNotExist())
                .andExpect(jsonPath("$.message").value("指定された API はありません。"));
        mvc.perform(get("/api/_probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("""
                        {"msgId":null,"message":"システムエラーが発生しました。","field":null}
                        """, true));
    }
}
