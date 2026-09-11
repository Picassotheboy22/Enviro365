package com.enviro.assessment.junior.smsibi.exception;

import org.springframework.security.access.AccessDeniedException;

/**
 * Thrown when a signed-in user asks for data or an action they are not entitled to, e.g. another investor's
 * portfolio. Mapped to HTTP 403 by {@link GlobalExceptionHandler}, which shows this message to the user.
 */
public class AccessForbiddenException extends AccessDeniedException {

    public AccessForbiddenException(String message) {
        super(message);
    }
}
