package jp.sanseki.pm.common.util;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    public Clock systemClock(@Value("${pm.system.zone:Asia/Tokyo}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
