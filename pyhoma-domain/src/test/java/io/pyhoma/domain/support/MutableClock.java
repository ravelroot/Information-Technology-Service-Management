package io.pyhoma.domain.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class MutableClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public MutableClock(LocalDateTime start) {
        this.zone = ZoneId.of("America/Sao_Paulo");
        this.instant = start.atZone(zone).toInstant();
    }

    public void advance(Duration d) {
        instant = instant.plus(d);
    }

    public void set(LocalDateTime t) {
        instant = t.atZone(zone).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId z) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
