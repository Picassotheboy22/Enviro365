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
 *
 * <p>Money for open withdrawal notices is put on hold rather than taken out straight away. It stays part of the balance
 * until the notice is paid, but it cannot be promised to another notice in the meantime:
 * <pre>
 *   available balance = balance - held amount
 * </pre>
 * The methods that change these amounts are package-private, so only {@link WithdrawalNotice} (in the same package) can
 * call them. The only way to move money is through the notice workflow.
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

    // The total of this product's open (pending or approved) withdrawal notices.
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal heldAmount = BigDecimal.ZERO.setScale(2);

    // Optimistic locking: every hold, release and payment changes this row. If two requests change the same product at
    // the same time, the second commit fails (and the user is asked to retry) instead of silently overwriting the
    // first. This is what stops two notices submitted at the same moment from both using the same money.
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
     * Puts money on hold for a new notice. The amount has already been checked against the business rules (see
     * WithdrawalPolicy). The guards in these methods are a last line of defence for the invariant
     * "0 &lt;= held amount &lt;= balance", even if a future caller forgets to apply the rules first.
     */
    void hold(BigDecimal amount) {
        requirePositive(amount);
        if (amount.compareTo(getAvailableBalance()) > 0) {
            throw new IllegalStateException(
                    "Cannot hold " + amount + ": only " + getAvailableBalance() + " is available");
        }
        heldAmount = heldAmount.add(amount);
    }

    /** Returns held money to the available balance, because its notice was rejected or cancelled. */
    void releaseHold(BigDecimal amount) {
        requirePositive(amount);
        if (amount.compareTo(heldAmount) > 0) {
            throw new IllegalStateException("Cannot release " + amount + ": only " + heldAmount + " is on hold");
        }
        heldAmount = heldAmount.subtract(amount);
    }

    /** Pays out held money. It leaves the hold and the balance together, so the available balance does not change. */
    void payOut(BigDecimal amount) {
        releaseHold(amount);
        balance = balance.subtract(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive, but was " + amount);
        }
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

    public BigDecimal getHeldAmount() {
        return heldAmount;
    }

    /** The part of the balance that is not on hold: what a new withdrawal notice can draw on. */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(heldAmount);
    }
}
