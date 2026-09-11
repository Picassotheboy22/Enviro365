package com.enviro.assessment.junior.smsibi.entity;

/**
 * What a signed-in user is allowed to do. Stored as a string column so re-ordering constants is safe.
 */
public enum Role {

    /** An investor: can view, and withdraw from, their own portfolio only. */
    INVESTOR,

    /** Enviro365 staff: can view every investor's portfolio and history, but cannot submit withdrawals. */
    ADMIN
}
