package com.enviro.assessment.junior.smsibi.entity;

/**
 * The kinds of investment product an investor can hold.
 *
 * <p>The type matters because the business rules differ: retirement products carry an age restriction,
 * savings products do not. Stored as a string (see {@code @Enumerated(EnumType.STRING)} on {@link Product})
 * so re-ordering these constants can never corrupt existing rows.
 */
public enum ProductType {

    /** Retirement products (e.g. retirement annuity, preservation fund) - only withdrawable by investors older than 65. */
    RETIREMENT,

    /** Discretionary savings / investment products - no age restriction. */
    SAVINGS
}
