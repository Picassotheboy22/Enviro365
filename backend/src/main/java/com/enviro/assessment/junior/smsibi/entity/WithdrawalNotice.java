package com.enviro.assessment.junior.smsibi.entity;

import com.enviro.assessment.junior.smsibi.exception.InvalidStatusTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A request by an investor to withdraw money from one of their {@link Product}s.
 *
 * <p>A notice is not processed on the spot. It goes through a workflow (see {@link NoticeStatus}): it is submitted as
 * PENDING with its amount on hold, approved by staff, and finally paid, which is when the money leaves the balance.
 * The workflow methods below are the only way to change a notice, and each one checks that the move is allowed, so a
 * notice can never skip a step or be paid twice.
 */
@Entity
@Table(name = "withdrawal_notices")
public class WithdrawalNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeStatus status;

    // Snapshots of the product balance when the notice is paid (null until then). They are stored rather than
    // recalculated later, because the balance keeps changing, but a statement must show what the balance was at the
    // time of each payment.
    @Column(precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    /** When the investor submitted the notice. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Who approved or rejected the notice, and when. The staff username is kept for the audit trail.
    @Column(length = 100)
    private String reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 100)
    private String paidBy;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

    // Optimistic locking: if two staff members act on the same notice at once (say one approves while the other
    // rejects), the second commit fails instead of silently overwriting the first.
    @Version
    private Long version;

    /** Required by JPA; not for application use. */
    protected WithdrawalNotice() {}

    private WithdrawalNotice(Product product, BigDecimal amount, LocalDateTime createdAt) {
        this.product = product;
        this.amount = amount;
        this.createdAt = createdAt;
        this.status = NoticeStatus.PENDING;
    }

    /**
     * Creates a pending notice and puts its amount on hold, so the same money cannot be promised to two notices. The
     * caller must check the business rules first (see WithdrawalPolicy).
     */
    public static WithdrawalNotice submit(Product product, BigDecimal amount, LocalDateTime submittedAt) {
        product.hold(amount);
        return new WithdrawalNotice(product, amount, submittedAt);
    }

    /** Staff approve the notice. The amount stays on hold until the notice is paid. */
    public void approve(String staffUsername, LocalDateTime at) {
        moveTo(NoticeStatus.APPROVED);
        reviewedBy = staffUsername;
        reviewedAt = at;
    }

    /** Staff turn the notice down. The held amount becomes available again. */
    public void reject(String staffUsername, LocalDateTime at, String reason) {
        moveTo(NoticeStatus.REJECTED);
        product.releaseHold(amount);
        reviewedBy = staffUsername;
        reviewedAt = at;
        rejectionReason = reason;
    }

    /** The investor withdraws the notice before staff have reviewed it. The held amount becomes available again. */
    public void cancel(LocalDateTime at) {
        moveTo(NoticeStatus.CANCELLED);
        product.releaseHold(amount);
        cancelledAt = at;
    }

    /** Staff record the payment: the amount now leaves the product balance, and the balances are kept as snapshots. */
    public void markPaid(String staffUsername, LocalDateTime at) {
        moveTo(NoticeStatus.PAID);
        balanceBefore = product.getBalance();
        product.payOut(amount);
        balanceAfter = product.getBalance();
        paidBy = staffUsername;
        paidAt = at;
    }

    private void moveTo(NoticeStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidStatusTransitionException(id, status, next);
        }
        status = next;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public NoticeStatus getStatus() {
        return status;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getPaidBy() {
        return paidBy;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
