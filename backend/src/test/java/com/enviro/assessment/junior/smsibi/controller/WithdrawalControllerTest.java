package com.enviro.assessment.junior.smsibi.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enviro.assessment.junior.smsibi.dto.WithdrawalFilter;
import com.enviro.assessment.junior.smsibi.dto.WithdrawalResponse;
import com.enviro.assessment.junior.smsibi.entity.ProductType;
import com.enviro.assessment.junior.smsibi.exception.BusinessRuleException;
import com.enviro.assessment.junior.smsibi.exception.ResourceNotFoundException;
import com.enviro.assessment.junior.smsibi.exception.RuleViolation;
import com.enviro.assessment.junior.smsibi.security.AuthenticatedUser;
import com.enviro.assessment.junior.smsibi.security.LoginAttemptService;
import com.enviro.assessment.junior.smsibi.security.SecurityConfig;
import com.enviro.assessment.junior.smsibi.security.TestUsers;
import com.enviro.assessment.junior.smsibi.service.CsvExportService;
import com.enviro.assessment.junior.smsibi.service.WithdrawalService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests: the MVC infrastructure, this controller, the GlobalExceptionHandler and the real SecurityConfig.
 * Services are mocked. They check the HTTP contract (status codes, headers, JSON errors, security rules), not the
 * business logic.
 */
@WebMvcTest(WithdrawalController.class)
@Import(SecurityConfig.class)
class WithdrawalControllerTest {

    private static final AuthenticatedUser INVESTOR = TestUsers.investor(7L);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WithdrawalService withdrawalService;

    @MockitoBean
    private CsvExportService csvExportService;

    // Needed by SecurityConfig; the sign-in flow itself is covered by AuthIntegrationTest.
    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @Test
    void createReturns201WithLocationHeaderAndBody() throws Exception {
        when(withdrawalService.createWithdrawal(any(), eq(INVESTOR))).thenReturn(sampleWithdrawal());

        mockMvc.perform(post("/api/withdrawals")
                        .with(user(INVESTOR))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 3, "amount": 2500.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/withdrawals/42")))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.balanceAfter").value(7500.00));
    }

    @Test
    void invalidBodyReturns400WithAnErrorPerField() throws Exception {
        mockMvc.perform(post("/api/withdrawals")
                        .with(user(INVESTOR))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": -5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.productId").value("Product is required"))
                .andExpect(jsonPath("$.errors.amount").value("Amount must be at least R 0.01"));

        verifyNoInteractions(withdrawalService);
    }

    @Test
    void tooManyDecimalPlacesReturns400() throws Exception {
        mockMvc.perform(post("/api/withdrawals")
                        .with(user(INVESTOR))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 3, "amount": 10.555}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("Amount can have at most 2 decimal places"));
    }

    @Test
    void malformedJsonReturns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/withdrawals")
                        .with(user(INVESTOR))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void businessRuleViolationReturns422WithCode() throws Exception {
        when(withdrawalService.createWithdrawal(any(), any()))
                .thenThrow(new BusinessRuleException(
                        RuleViolation.EXCEEDS_WITHDRAWAL_LIMIT, "Withdrawals may not exceed 90% of the balance."));

        mockMvc.perform(post("/api/withdrawals")
                        .with(user(INVESTOR))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 3, "amount": 9500.00}
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("EXCEEDS_WITHDRAWAL_LIMIT"))
                .andExpect(jsonPath("$.detail").value("Withdrawals may not exceed 90% of the balance."));
    }

    @Test
    void unknownProductReturns404() throws Exception {
        when(withdrawalService.createWithdrawal(any(), any()))
                .thenThrow(new ResourceNotFoundException("Product", 999L));

        mockMvc.perform(post("/api/withdrawals")
                        .with(user(INVESTOR))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 999, "amount": 10.00}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product with id 999 was not found."));
    }

    @Test
    void historyBindsQueryParametersToTheFilterAndPassesTheSignedInUser() throws Exception {
        when(withdrawalService.findWithdrawals(any(), any())).thenReturn(List.of(sampleWithdrawal()));

        mockMvc.perform(get("/api/withdrawals")
                        .with(user(INVESTOR))
                        .param("investorId", "7")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("Unit Trust Portfolio"));

        verify(withdrawalService)
                .findWithdrawals(
                        eq(new WithdrawalFilter(7L, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))),
                        eq(INVESTOR));
    }

    @Test
    void invertedDateRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/withdrawals")
                        .with(user(INVESTOR))
                        .param("from", "2026-02-01")
                        .param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dateRangeValid").value("'From' date must be on or before 'To' date"));

        verifyNoInteractions(withdrawalService);
    }

    @Test
    void unparseableDateReturns400() throws Exception {
        mockMvc.perform(get("/api/withdrawals").with(user(INVESTOR)).param("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.from").value("Invalid value"));
    }

    @Test
    void exportReturnsCsvAsAttachment() throws Exception {
        when(withdrawalService.findWithdrawals(any(), any())).thenReturn(List.of(sampleWithdrawal()));
        when(csvExportService.export(any()))
                .thenReturn(new CsvExportService.CsvFile(
                        "withdrawal-statement-2026-09-10.csv", "Notice ID\r\n42\r\n".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/withdrawals/export").with(user(INVESTOR)).param("investorId", "7"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(
                                "Content-Disposition", "attachment; filename=\"withdrawal-statement-2026-09-10.csv\""))
                .andExpect(content().string("Notice ID\r\n42\r\n"));
    }

    // ---- security rules ----

    @Test
    void requestsWithoutASessionGet401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/withdrawals"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Please sign in to continue."));

        verifyNoInteractions(withdrawalService);
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/withdrawals")
                        .with(user(INVESTOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 3, "amount": 10.00}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Invalid CSRF token"));

        verifyNoInteractions(withdrawalService);
    }

    @Test
    void staffAccountsCannotSubmitWithdrawals() throws Exception {
        mockMvc.perform(post("/api/withdrawals")
                        .with(user(TestUsers.admin()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 3, "amount": 10.00}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));

        verifyNoInteractions(withdrawalService);
    }

    private static WithdrawalResponse sampleWithdrawal() {
        return new WithdrawalResponse(
                42L,
                7L,
                "Lerato Dlamini",
                3L,
                "Unit Trust Portfolio",
                ProductType.SAVINGS,
                new BigDecimal("2500.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("7500.00"),
                LocalDateTime.of(2026, 9, 10, 10, 0));
    }
}
