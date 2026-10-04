package ar.edu.utn.frba.tps.monolith.messaging.service;

import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalAccount;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalRole;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalSession;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalSessionServiceTest {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final Instant START = Instant.parse("2026-10-04T12:00:00Z");

    private final TestClock clock = new TestClock(START);
    private final TechnicalSessionService sessionService = new TechnicalSessionService(TTL, clock);
    private final TechnicalAccount account = new TechnicalAccount("lector", "hash", TechnicalRole.READ, "SERVICE");

    @Test
    void issuedTokenResolvesToTheSessionOfTheAccount() {
        TechnicalSession issued = sessionService.issue(account);

        assertThat(sessionService.resolve(issued.token())).hasValueSatisfying(session -> {
            assertThat(session.username()).isEqualTo("lector");
            assertThat(session.role()).isEqualTo(TechnicalRole.READ);
            assertThat(session.expiresAt()).isEqualTo(START.plus(TTL));
        });
    }

    @Test
    void unknownTokenDoesNotResolve() {
        sessionService.issue(account);

        assertThat(sessionService.resolve("unknown")).isEmpty();
    }

    @Test
    void tokenStopsResolvingWhenItExpires() {
        TechnicalSession issued = sessionService.issue(account);

        clock.advance(TTL.minusSeconds(1));
        assertThat(sessionService.resolve(issued.token())).isPresent();

        clock.advance(Duration.ofSeconds(1));
        assertThat(sessionService.resolve(issued.token())).isEmpty();
    }

    @Test
    void everyLoginGetsADifferentToken() {
        String first = sessionService.issue(account).token();
        String second = sessionService.issue(account).token();

        assertThat(first).isNotEqualTo(second);
        assertThat(sessionService.resolve(first)).isPresent();
        assertThat(sessionService.resolve(second)).isPresent();
    }

    private static final class TestClock extends Clock {

        private Instant now;

        private TestClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

    }

}
