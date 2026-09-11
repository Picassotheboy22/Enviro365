package com.enviro.assessment.junior.smsibi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A record of a withdrawal made against a {@link Product}.
 *
 * <p>The balance before and after are stored as snapshots rather than recalculated later. The product's
 * balance keeps changing with every new withdrawal, but a statement must show what the balance was
 * <em>at the time</em> of each withdrawal.
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

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Required by JPA; not for application use. */
    protected WithdrawalNotice() {}

    public WithdrawalNotice(
            Product product,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            LocalDateTime createdAt) {
        this.product = product;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
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

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
