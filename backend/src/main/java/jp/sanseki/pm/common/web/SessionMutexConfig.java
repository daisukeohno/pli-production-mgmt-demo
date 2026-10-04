package jp.sanseki.pm.common.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.HttpSessionMutexListener;

/** 端末（HTTP セッション）単位の直列化に使う安定したミューテックスをセッションに登録する。 */
@Configuration
public class SessionMutexConfig {

    @Bean
    public HttpSessionMutexListener httpSessionMutexListener() {
        return new HttpSessionMutexListener();
    }
}
