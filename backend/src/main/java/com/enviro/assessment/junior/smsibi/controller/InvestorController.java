package com.enviro.assessment.junior.smsibi.controller;

import com.enviro.assessment.junior.smsibi.dto.InvestorSummary;
import com.enviro.assessment.junior.smsibi.dto.PortfolioResponse;
import com.enviro.assessment.junior.smsibi.security.AuthenticatedUser;
import com.enviro.assessment.junior.smsibi.service.InvestorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controllers are kept thin: they only translate HTTP to service calls and back. Logic lives in the service layer and
 * error handling in GlobalExceptionHandler. {@code @AuthenticationPrincipal} injects the signed-in user from the session.
 */
@RestController
@RequestMapping("/api/investors")
@Tag(name = "Investors", description = "Investor details and portfolios")
public class InvestorController {

    private final InvestorService investorService;

    public InvestorController(InvestorService investorService) {
        this.investorService = investorService;
    }

    @GetMapping
    @Operation(summary = "List all investors (staff only)")
    @ApiResponse(responseCode = "200", description = "All investors")
    @ApiResponse(responseCode = "403", description = "Signed in as an investor rather than staff")
    public List<InvestorSummary> listInvestors(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return investorService.listInvestors(user);
    }

    @GetMapping("/{investorId}/portfolio")
    @Operation(summary = "Get an investor's portfolio (personal details and products)")
    @ApiResponse(responseCode = "200", description = "Portfolio found")
    @ApiResponse(responseCode = "403", description = "Another investor's portfolio")
    @ApiResponse(responseCode = "404", description = "Investor does not exist")
    public PortfolioResponse getPortfolio(
            @PathVariable Long investorId, @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return investorService.getPortfolio(investorId, user);
    }
}
