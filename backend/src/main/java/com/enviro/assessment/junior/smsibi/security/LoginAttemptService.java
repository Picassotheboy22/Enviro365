package com.enviro.assessment.junior.smsibi.security;

import com.enviro.assessment.junior.smsibi.entity.UserAccount;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Brute-force protection: after {@code max-failures} failed sign-ins, a username is locked for
 * {@code lock-minutes} (OWASP recommends throttling or temporary lock-outs against password guessing).
 *
 * <p>Failures are counted per username <em>whether or not the account exists</em>. Real and made-up usernames
 * therefore behave identically, so the lock-out cannot be used to discover which e-mail addresses have accounts.
 *
 * <p>State is kept in memory. That is fine for a single server; several servers would need a shared store such as
 * Redis or the database.
 */
@Service
public class LoginAttemptService {

    private final int maxFailures;
    private final Duration lockDuration;
    private final Clock clock;
    // Many sign-in requests can run at the same time, so each update of a counter must be atomic:
    // ConcurrentHashMap.compute() guarantees that.
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.security.login.max-failures:5}") int maxFailures,
            @Value("${app.security.login.lock-minutes:15}") long lockMinutes,
            Clock clock) {
        this.maxFailures = maxFailures;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
        this.clock = clock;
    }

    public boolean isBlocked(String username) {
        return remainingLock(username).isPresent();
    }

    /** How much longer the username stays locked, or empty if it is not locked. */
    public Optional<Duration> remainingLock(String username) {
        Attempts current = attempts.get(UserAccount.normaliseUsername(username));
        Instant now = clock.instant();
        if (current == null || !current.isLockedAt(now)) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, current.lockedUntil()));
    }

    public void recordFailure(String username) {
        Instant now = clock.instant();
        attempts.compute(UserAccount.normaliseUsername(username), (key, current) -> {
            // First failure, or a previous lock that has run out: start counting from zero again.
            Attempts base = (current == null || current.lockHasExpiredAt(now)) ? Attempts.NONE : current;
            int failures = base.failures() + 1;
            Instant lockedUntil = failures >= maxFailures ? now.plus(lockDuration) : null;
            return new Attempts(failures, lockedUntil);
        });
    }

    /** A successful sign-in clears the failure count. */
    public void recordSuccess(String username) {
        attempts.remove(UserAccount.normaliseUsername(username));
    }

    private record Attempts(int failures, Instant lockedUntil) {

        static final Attempts NONE = new Attempts(0, null);

        boolean isLockedAt(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        boolean lockHasExpiredAt(Instant now) {
            return lockedUntil != null && !now.isBefore(lockedUntil);
        }
    }
}
