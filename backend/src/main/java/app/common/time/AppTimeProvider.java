package app.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AppTimeProvider {

    private final Clock utcClock;
    private final Clock seoulClock;

    public AppTimeProvider(@Qualifier("utcClock") Clock utcClock, @Qualifier("seoulClock") Clock seoulClock) {
        this.utcClock = utcClock;
        this.seoulClock = seoulClock;
    }

    public Instant nowInstant() {
        return Instant.now(utcClock);
    }

    public LocalDateTime nowUtcDateTime() {
        return LocalDateTime.ofInstant(nowInstant(), ZoneOffset.UTC);
    }

    public LocalDate todaySeoul() {
        return LocalDate.now(seoulClock);
    }
}
