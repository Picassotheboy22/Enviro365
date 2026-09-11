package com.enviro.assessment.junior.smsibi.entity;

/**
 * What a signed-in user is allowed to do. Stored as a string column so re-ordering constants is safe.
 */
public enum Role {

    /** An investor: can view their own portfolio, and submit and cancel withdrawal notices on it. */
    INVESTOR,

    /**
     * Enviro365 staff: can view every investor's portfolio and history, and approve, reject and pay withdrawal notices.
     * Staff cannot submit withdrawals themselves.
     */
    ADMIN
}
