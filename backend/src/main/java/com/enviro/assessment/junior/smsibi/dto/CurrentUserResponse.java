package com.enviro.assessment.junior.smsibi.dto;

import com.enviro.assessment.junior.smsibi.entity.Role;

/**
 * The signed-in user, as returned by {@code GET /api/auth/me}. The UI uses it to decide what to show
 * (e.g. the investor picker is only for ADMIN). The server still enforces every rule itself.
 *
 * @param investorId the user's own investor id, or {@code null} for staff accounts
 */
public record CurrentUserResponse(String username, String displayName, Role role, Long investorId) {}
