package com.enviro.assessment.junior.smsibi.controller;

import com.enviro.assessment.junior.smsibi.dto.CurrentUserResponse;
import com.enviro.assessment.junior.smsibi.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session helpers for the React app. Signing in ({@code POST /api/auth/login}) and out
 * ({@code POST /api/auth/logout}) are handled by Spring Security filters configured in SecurityConfig, so they have
 * no controller methods here.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "CSRF token and the signed-in user")
public class AuthController {

    /**
     * Makes sure the browser holds a CSRF token cookie. Spring creates tokens lazily; reading the token here forces
     * the XSRF-TOKEN cookie to be written. The UI calls this before its first POST (the sign-in form).
     */
    @GetMapping("/csrf")
    @Operation(summary = "Issue a CSRF token cookie (XSRF-TOKEN)")
    @ApiResponse(responseCode = "204", description = "Cookie set")
    public ResponseEntity<Void> csrfToken(@Parameter(hidden = true) CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "The signed-in user")
    @ApiResponse(responseCode = "200", description = "Signed in")
    @ApiResponse(responseCode = "401", description = "Not signed in, or the session has expired")
    public CurrentUserResponse currentUser(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return new CurrentUserResponse(user.getUsername(), user.getDisplayName(), user.getRole(), user.getInvestorId());
    }
}
