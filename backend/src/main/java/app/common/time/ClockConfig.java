package app.common.time;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    public static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Bean
    @Qualifier("utcClock")
    public Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    @Qualifier("seoulClock")
    public Clock seoulClock() {
        return Clock.system(SEOUL_ZONE_ID);
    }
}
