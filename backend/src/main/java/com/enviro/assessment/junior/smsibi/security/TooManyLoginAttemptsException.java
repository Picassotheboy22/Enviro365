package com.enviro.assessment.junior.smsibi.security;

import java.io.Serial;
import java.time.Duration;
import org.springframework.security.core.AuthenticationException;

/**
 * A sign-in was refused because the username is temporarily locked. Mapped to HTTP 429 (Too Many Requests) with a
 * {@code Retry-After} header.
 */
public class TooManyLoginAttemptsException extends AuthenticationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super("Too many failed sign-in attempts");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
