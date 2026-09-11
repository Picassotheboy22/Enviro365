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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Locale;

/**
 * A sign-in account.
 *
 * <p>Kept separate from {@link Investor} for two reasons: not every user is an investor (staff accounts have no
 * portfolio), and credentials are a security concern that should not be mixed into investment data.
 */
@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Sign-in name (an e-mail address), stored in lower case so signing in is case-insensitive. */
    @Column(nullable = false, unique = true, length = 150)
    private String username;

    /**
     * A one-way hash of the password, never the password itself, e.g. "{bcrypt}$2a$10$...". The prefix records
     * the algorithm, so it can be upgraded later without invalidating existing passwords.
     */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** The investor this account belongs to; {@code null} for staff (ADMIN) accounts. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investor_id", unique = true)
    private Investor investor;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Required by JPA; not for application use. */
    protected UserAccount() {}

    private UserAccount(String username, String passwordHash, String displayName, Role role, Investor investor) {
        this.username = normaliseUsername(username);
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.investor = investor;
    }

    /** An investor's account. Their e-mail address is their username. */
    public static UserAccount forInvestor(Investor investor, String passwordHash) {
        return new UserAccount(investor.getEmail(), passwordHash, investor.getFullName(), Role.INVESTOR, investor);
    }

    /** A staff account with read-only access to every investor. */
    public static UserAccount staff(String username, String displayName, String passwordHash) {
        return new UserAccount(username, passwordHash, displayName, Role.ADMIN, null);
    }

    /** "  Thabo@Example.com " and "thabo@example.com" are the same user. */
    public static String normaliseUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public Investor getInvestor() {
        return investor;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
