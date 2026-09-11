package com.enviro.assessment.junior.smsibi.exception;

import com.enviro.assessment.junior.smsibi.security.SecurityConfig;
import com.enviro.assessment.junior.smsibi.security.TooManyLoginAttemptsException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every exception into a consistent JSON error body, so controllers never need try/catch blocks.
 *
 * <p>Errors use the RFC 9457 "Problem Details" format ({@code type, title, status, detail, instance}) that Spring
 * supports out of the box, plus {@code code} for business rules and {@code errors} (field -> message) for validation.
 *
 * <p>Security errors come through here too: Spring Security's entry point, access-denied handler and sign-in failure
 * handler forward to this class (see SecurityConfig), so a 401 or 403 looks exactly like any other error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        return problem;
    }

    /** 422 rather than 400: the request was syntactically valid, but the rules do not allow it. */
    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Withdrawal not allowed");
        problem.setProperty("code", ex.getViolation().name());
        return problem;
    }

    /** Raised by the {@code @Version} check when two requests updated the same product at the same time. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentUpdate(OptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "This product was updated by another request at the same time. Please refresh and try again.");
        problem.setTitle("Concurrent update");
        return problem;
    }

    /**
     * 401: not signed in (or the session expired), or a failed sign-in. The sign-in message is deliberately vague:
     * it never says whether the username exists or the password was wrong, because that would help an attacker find
     * valid accounts.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        boolean signInAttempt = request.getRequestURI().endsWith(SecurityConfig.LOGIN_URL);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                signInAttempt ? "Invalid username or password." : "Please sign in to continue.");
        problem.setTitle(signInAttempt ? "Sign-in failed" : "Unauthorized");
        return problem;
    }

    /** 429: the username is temporarily locked after too many failed sign-ins. */
    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyLoginAttempts(TooManyLoginAttemptsException ex) {
        long seconds = Math.max(1, ex.getRetryAfter().toSeconds());
        long minutes = (seconds + 59) / 60; // round up, so the user never sees "0 minutes"
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many failed sign-in attempts. Please try again in " + minutes
                        + (minutes == 1 ? " minute." : " minutes."));
        problem.setTitle("Account temporarily locked");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(problem);
    }

    /** 403: signed in but not allowed (another investor's data, staff trying to withdraw), or a bad CSRF token. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        if (ex instanceof CsrfException) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Your security token is missing or has expired. Please refresh the page and try again.");
            problem.setTitle("Invalid CSRF token");
            return problem;
        }
        // Only our own messages are shown to users; framework messages such as "Access Denied" are replaced.
        String detail =
                ex instanceof AccessForbiddenException ? ex.getMessage() : "You do not have permission to do that.";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
        problem.setTitle("Access denied");
        return problem;
    }

    /** Bean Validation failures ({@code @Valid} on a request body or on query filters). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            // A "binding failure" means the value could not even be converted (e.g. amount=abc). Spring's default
            // message for that is technical, so replace it with something readable.
            String message = error.isBindingFailure() ? "Invalid value" : error.getDefaultMessage();
            errors.putIfAbsent(error.getField(), message);
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage());
        }

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /**
     * Safety net for anything unexpected. The real cause is logged for developers, but only a generic message is
     * returned, so internal details (SQL, stack traces) are never exposed to clients.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again later.");
        problem.setTitle("Internal server error");
        return problem;
    }
}
