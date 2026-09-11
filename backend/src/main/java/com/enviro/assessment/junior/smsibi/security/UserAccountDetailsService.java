package com.enviro.assessment.junior.smsibi.security;

import com.enviro.assessment.junior.smsibi.entity.UserAccount;
import com.enviro.assessment.junior.smsibi.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tells Spring Security how to find a user by username. Spring then checks the password against the stored
 * BCrypt hash itself, so this class never sees or compares passwords.
 */
@Service
public class UserAccountDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccounts;
    private final LoginAttemptService loginAttempts;

    public UserAccountDetailsService(UserAccountRepository userAccounts, LoginAttemptService loginAttempts) {
        this.userAccounts = userAccounts;
        this.loginAttempts = loginAttempts;
    }

    /**
     * A temporarily blocked username is returned as "locked". Spring Security then rejects the attempt before it
     * even checks the password, so guessing cannot continue while the lock lasts.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String normalised = UserAccount.normaliseUsername(username);
        UserAccount account = userAccounts
                .findByUsername(normalised)
                // Spring turns this into a generic "bad credentials" error, so callers cannot tell an
                // unknown username apart from a wrong password.
                .orElseThrow(() -> new UsernameNotFoundException("Unknown username"));
        return AuthenticatedUser.from(account, loginAttempts.isBlocked(normalised));
    }
}
