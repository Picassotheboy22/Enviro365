package com.enviro.assessment.junior.smsibi.security;

import com.enviro.assessment.junior.smsibi.entity.Role;
import com.enviro.assessment.junior.smsibi.entity.UserAccount;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The signed-in user, as kept in the HTTP session by Spring Security.
 *
 * <p>It holds what authorisation decisions need (role and investor id), so a service can answer
 * "may this user see investor 3?" without another database query. It implements
 * {@link CredentialsContainer}, so Spring Security wipes the password hash straight after sign-in:
 * the hash has no reason to stay in memory for the rest of the session.
 */
public class AuthenticatedUser implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private String passwordHash;
    private final String displayName;
    private final Role role;
    private final Long investorId;
    private final boolean enabled;
    private final boolean locked;

    public AuthenticatedUser(
            Long userId,
            String username,
            String passwordHash,
            String displayName,
            Role role,
            Long investorId,
            boolean enabled,
            boolean locked) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.investorId = investorId;
        this.enabled = enabled;
        this.locked = locked;
    }

    /**
     * @param locked true while the username is temporarily blocked after too many failed sign-ins
     */
    public static AuthenticatedUser from(UserAccount account, boolean locked) {
        Long investorId =
                account.getInvestor() == null ? null : account.getInvestor().getId();
        return new AuthenticatedUser(
                account.getId(),
                account.getUsername(),
                account.getPasswordHash(),
                account.getDisplayName(),
                account.getRole(),
                investorId,
                account.isEnabled(),
                locked);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** Staff can see every investor; an investor can see only themselves. */
    public boolean canAccessInvestor(Long requestedInvestorId) {
        return isAdmin() || (investorId != null && investorId.equals(requestedInvestorId));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring's hasRole("ADMIN") looks for an authority called "ROLE_ADMIN".
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public Long getInvestorId() {
        return investorId;
    }
}
