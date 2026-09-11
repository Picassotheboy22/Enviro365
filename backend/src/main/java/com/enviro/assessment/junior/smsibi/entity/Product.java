package com.enviro.assessment.junior.smsibi.entity;

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

/**
 * An investment product (e.g. a retirement annuity or savings account) held by an {@link Investor}.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY so that loading a product does not automatically load its investor unless it is actually used.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private Investor investor;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductType type;

    // BigDecimal (never double) for money: binary floating point cannot represent cents exactly,
    // e.g. 0.1 + 0.2 != 0.3 as doubles.
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    // Optimistic locking: if two withdrawals on the same product are processed at the same time, the second
    // commit fails (and the user is asked to retry) instead of silently overwriting the first one's balance.
    @Version
    private Long version;

    /** Required by JPA; not for application use. */
    protected Product() {}

    public Product(Investor investor, String name, ProductType type, BigDecimal balance) {
        this.investor = investor;
        this.name = name;
        this.type = type;
        this.balance = balance;
    }

    /**
     * Deducts an amount that has already been checked against the business rules (see WithdrawalPolicy).
     * The guard below is a last line of defence for the invariant "a balance never goes negative",
     * even if a future caller forgets to apply the rules first.
     */
    public void withdraw(BigDecimal amount) {
        if (amount.signum() <= 0 || amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Invalid withdrawal amount " + amount + " for balance " + balance);
        }
        this.balance = this.balance.subtract(amount);
    }

    public Long getId() {
        return id;
    }

    public Investor getInvestor() {
        return investor;
    }

    public String getName() {
        return name;
    }

    public ProductType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
