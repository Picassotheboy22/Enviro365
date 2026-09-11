package com.enviro.assessment.junior.smsibi.dto;

import com.enviro.assessment.junior.smsibi.entity.NoticeStatus;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Optional query-string filters, shared by the history endpoint and the CSV export.
 * Sharing one type guarantees the downloaded CSV contains exactly the rows the user sees in the table.
 *
 * <p>Spring binds query parameters straight onto the record components, e.g.
 * {@code /api/withdrawals?investorId=1&from=2026-01-01&to=2026-03-31&status=PENDING&status=APPROVED}. A repeated
 * parameter becomes a list.
 */
public record WithdrawalFilter(
        @Parameter(description = "Only notices for this investor")
        Long investorId,

        @Parameter(description = "Only notices for this product")
        Long productId,

        @Parameter(description = "Submitted on or after this date (yyyy-MM-dd)")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @Parameter(description = "Submitted on or before this date (yyyy-MM-dd)")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,

        @Parameter(description = "Only notices with one of these statuses. Repeat the parameter for several.")
        List<NoticeStatus> status) {

    /** No filters at all: returns every withdrawal notice. */
    public static WithdrawalFilter none() {
        return new WithdrawalFilter(null, null, null, null, null);
    }

    // Cross-field rule that a single-field annotation cannot express. Bean Validation treats this "isXxx"
    // method as a property, so a failure is reported like any other field error (key: "dateRangeValid").
    @Schema(hidden = true)
    @AssertTrue(message = "'From' date must be on or before 'To' date")
    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}
