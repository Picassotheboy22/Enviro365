package com.enviro.assessment.junior.smsibi.exception;

/**
 * Thrown when a request is well-formed but breaks a business rule (e.g. withdrawing more than 90%).
 * Mapped to HTTP 422 (Unprocessable Content) by {@link GlobalExceptionHandler}, which distinguishes it from
 * 400 (malformed input) and lets the UI show the message to the user as-is.
 */
public class BusinessRuleException extends RuntimeException {

    private final RuleViolation violation;

    public BusinessRuleException(RuleViolation violation, String message) {
        super(message);
        this.violation = violation;
    }

    public RuleViolation getViolation() {
        return violation;
    }
}
