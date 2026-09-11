package com.enviro.assessment.junior.smsibi.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private static final String USER = "thabo@example.com";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-11T08:00:00Z"));
    // 3 failures lock the username for 15 minutes.
    private final LoginAttemptService service = new LoginAttemptService(3, 15, clock);

    @Test
    void locksTheUsernameAfterTheMaximumNumberOfFailures() {
        fail(USER, 2);
        assertThat(service.isBlocked(USER)).isFalse();

        fail(USER, 1);

        assertThat(service.isBlocked(USER)).isTrue();
        assertThat(service.remainingLock(USER)).contains(Duration.ofMinutes(15));
    }

    @Test
    void unlocksWhenTheLockExpires() {
        fail(USER, 3);
        clock.advance(Duration.ofMinutes(15));

        assertThat(service.isBlocked(USER)).isFalse();
    }

    @Test
    void aFailureAfterAnExpiredLockStartsANewCount() {
        fail(USER, 3);
        clock.advance(Duration.ofMinutes(16));

        fail(USER, 1);

        assertThat(service.isBlocked(USER)).isFalse();
    }

    @Test
    void successfulSignInResetsTheCount() {
        fail(USER, 2);
        service.recordSuccess(USER);
        fail(USER, 2);

        assertThat(service.isBlocked(USER)).isFalse();
    }

    @Test
    void usernamesAreCaseInsensitive() {
        fail("  Thabo@Example.COM ", 3);

        assertThat(service.isBlocked(USER)).isTrue();
    }

    @Test
    void unknownUsernamesAreTrackedTheSameWaySoTheyRevealNothing() {
        fail("nobody@example.com", 3);

        assertThat(service.isBlocked("nobody@example.com")).isTrue();
    }

    private void fail(String username, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(username);
        }
    }

    /** A clock the test can move forward, to check time-based behaviour without sleeping. */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
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
