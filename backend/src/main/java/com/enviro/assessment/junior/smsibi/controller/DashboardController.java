package com.enviro.assessment.junior.smsibi.controller;

import com.enviro.assessment.junior.smsibi.dto.DashboardResponse;
import com.enviro.assessment.junior.smsibi.security.AuthenticatedUser;
import com.enviro.assessment.junior.smsibi.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Statistics across all clients (staff only)")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Headline statistics for the staff dashboard")
    @ApiResponse(responseCode = "200", description = "Dashboard statistics")
    @ApiResponse(responseCode = "403", description = "Signed in as an investor rather than staff")
    public DashboardResponse getDashboard(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getDashboard(user);
    }
}
