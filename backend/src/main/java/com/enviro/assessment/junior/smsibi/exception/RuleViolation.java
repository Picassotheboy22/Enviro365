package com.enviro.assessment.junior.smsibi.exception;

/**
 * Machine-readable codes for business-rule failures. They are returned in the error response's
 * {@code code} field, so a client can react to a specific rule without parsing the human-readable message.
 */
public enum RuleViolation {
    INVALID_AMOUNT,
    RETIREMENT_AGE_RESTRICTION,
    INSUFFICIENT_BALANCE,
    EXCEEDS_WITHDRAWAL_LIMIT
}
