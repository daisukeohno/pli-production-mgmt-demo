package jp.co.sanseki.pm.common;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** GET-SYS-DATE（CICS ASKTIME）の置き換え。テストでは固定 Clock を注入して年またぎを再現する。 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${sanseki.zone-id:Asia/Tokyo}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
