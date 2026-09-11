package com.enviro.assessment.junior.smsibi.controller;

import com.enviro.assessment.junior.smsibi.dto.WithdrawalFilter;
import com.enviro.assessment.junior.smsibi.dto.WithdrawalRequest;
import com.enviro.assessment.junior.smsibi.dto.WithdrawalResponse;
import com.enviro.assessment.junior.smsibi.security.AuthenticatedUser;
import com.enviro.assessment.junior.smsibi.service.CsvExportService;
import com.enviro.assessment.junior.smsibi.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/withdrawals")
@Tag(name = "Withdrawals", description = "Submit withdrawal notices, view history and export CSV statements")
public class WithdrawalController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final WithdrawalService withdrawalService;
    private final CsvExportService csvExportService;

    public WithdrawalController(WithdrawalService withdrawalService, CsvExportService csvExportService) {
        this.withdrawalService = withdrawalService;
        this.csvExportService = csvExportService;
    }

    /**
     * REST convention for creating a resource: 201 Created, the created object in the body, and a {@code Location}
     * header pointing at its URL.
     */
    @PostMapping
    @Operation(summary = "Submit a withdrawal notice (investors only, own products only)")
    @ApiResponse(responseCode = "201", description = "Withdrawal accepted and balance updated")
    @ApiResponse(responseCode = "400", description = "Invalid input (missing product, non-positive amount, ...)")
    @ApiResponse(responseCode = "403", description = "Staff account, another investor's product, or missing CSRF token")
    @ApiResponse(responseCode = "404", description = "Product does not exist")
    @ApiResponse(responseCode = "422", description = "A business rule was broken (age, balance or 90% limit)")
    public ResponseEntity<WithdrawalResponse> createWithdrawal(
            @Valid @RequestBody WithdrawalRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        WithdrawalResponse created = withdrawalService.createWithdrawal(request, user);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single withdrawal notice")
    public WithdrawalResponse getWithdrawal(
            @PathVariable Long id, @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return withdrawalService.getWithdrawal(id, user);
    }

    @GetMapping
    @Operation(summary = "Withdrawal history, newest first, with optional filters (investors see only their own)")
    public List<WithdrawalResponse> listWithdrawals(
            @Valid @ParameterObject WithdrawalFilter filter,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return withdrawalService.findWithdrawals(filter, user);
    }

    /**
     * Same filters as the history endpoint. {@code Content-Disposition: attachment} tells the browser to download the
     * response as a file instead of displaying it.
     */
    @GetMapping("/export")
    @Operation(summary = "Download withdrawal history as a CSV statement")
    @ApiResponse(responseCode = "200", description = "CSV file")
    public ResponseEntity<byte[]> exportWithdrawals(
            @Valid @ParameterObject WithdrawalFilter filter,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        CsvExportService.CsvFile file = csvExportService.export(withdrawalService.findWithdrawals(filter, user));
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.filename())
                                .build()
                                .toString())
                .body(file.content());
    }
}
