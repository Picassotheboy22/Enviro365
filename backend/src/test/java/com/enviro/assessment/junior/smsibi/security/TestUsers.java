package com.enviro.assessment.junior.smsibi.security;

import com.enviro.assessment.junior.smsibi.entity.Role;

/** Ready-made signed-in users for tests. */
public final class TestUsers {

    private TestUsers() {}

    public static AuthenticatedUser investor(long investorId) {
        return new AuthenticatedUser(
                100 + investorId,
                "investor" + investorId + "@example.com",
                null,
                "Investor " + investorId,
                Role.INVESTOR,
                investorId,
                true,
                false);
    }

    public static AuthenticatedUser admin() {
        return new AuthenticatedUser(
                1L, "admin@enviro365.example", null, "Enviro365 Admin", Role.ADMIN, null, true, false);
    }
}
