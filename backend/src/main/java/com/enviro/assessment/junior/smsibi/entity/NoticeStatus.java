package com.enviro.assessment.junior.smsibi.entity;

/**
 * Where a withdrawal notice is in its workflow. Stored as a string column, so re-ordering the constants is safe.
 *
 * <pre>
 *   PENDING  -- staff approve ---&gt;  APPROVED  -- staff pay --&gt;  PAID
 *   PENDING  -- staff reject ----&gt;  REJECTED
 *   PENDING  -- investor cancels -&gt; CANCELLED
 * </pre>
 *
 * <p>Every allowed move is listed in {@link #canMoveTo}, so the whole workflow can be read, and tested, in one place.
 */
public enum NoticeStatus {

    /** Submitted by the investor and waiting for staff to review it. The amount is on hold. */
    PENDING,

    /** Approved by staff and waiting to be paid out. The amount is still on hold. */
    APPROVED,

    /** Paid out: the amount has left the product balance. Final. */
    PAID,

    /** Turned down by staff, with a reason for the investor. The hold is released. Final. */
    REJECTED,

    /** Withdrawn by the investor before staff reviewed it. The hold is released. Final. */
    CANCELLED;

    public boolean canMoveTo(NoticeStatus next) {
        return switch (this) {
            case PENDING -> next == APPROVED || next == REJECTED || next == CANCELLED;
            case APPROVED -> next == PAID;
            case PAID, REJECTED, CANCELLED -> false;
        };
    }

    /** Open notices (pending or approved) still hold money on their product. */
    public boolean isOpen() {
        return this == PENDING || this == APPROVED;
    }
}
