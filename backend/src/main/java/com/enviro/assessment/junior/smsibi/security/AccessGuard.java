package com.enviro.assessment.junior.smsibi.security;

import com.enviro.assessment.junior.smsibi.dto.WithdrawalFilter;
import com.enviro.assessment.junior.smsibi.exception.AccessForbiddenException;

/**
 * Ownership checks: "is this record yours?".
 *
 * <p>The URL rules in {@link SecurityConfig} decide which <em>kinds</em> of request each role may make. These checks
 * decide which <em>records</em> a user may touch. Without them, an investor could read someone else's portfolio just by
 * changing the id in the URL (OWASP calls this an "insecure direct object reference").
 */
public final class AccessGuard {

    private AccessGuard() {}

    public static void requireInvestorAccess(AuthenticatedUser user, Long investorId) {
        if (!user.canAccessInvestor(investorId)) {
            throw new AccessForbiddenException("You can only access your own portfolio.");
        }
    }

    /**
     * Investors always see only their own withdrawals, whatever filter they send. Staff may filter by any investor,
     * or none.
     */
    public static WithdrawalFilter restrictToUser(WithdrawalFilter filter, AuthenticatedUser user) {
        if (user.isAdmin()) {
            return filter;
        }
        if (user.getInvestorId() == null
                || (filter.investorId() != null && !filter.investorId().equals(user.getInvestorId()))) {
            throw new AccessForbiddenException("You can only access your own withdrawals.");
        }
        return new WithdrawalFilter(user.getInvestorId(), filter.productId(), filter.from(), filter.to());
    }
}
