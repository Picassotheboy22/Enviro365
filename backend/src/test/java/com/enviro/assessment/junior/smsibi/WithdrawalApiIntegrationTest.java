package com.enviro.assessment.junior.smsibi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.oneOf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enviro.assessment.junior.smsibi.config.DataSeeder;
import com.enviro.assessment.junior.smsibi.entity.Investor;
import com.enviro.assessment.junior.smsibi.entity.Product;
import com.enviro.assessment.junior.smsibi.entity.ProductType;
import com.enviro.assessment.junior.smsibi.repository.InvestorRepository;
import com.enviro.assessment.junior.smsibi.repository.ProductRepository;
import com.enviro.assessment.junior.smsibi.security.AuthenticatedUser;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * End-to-end tests through every layer (security, HTTP, controller, service, JPA, H2) using DataSeeder's demo data.
 * Each request runs as a real seeded user, loaded the same way Spring Security loads it at sign-in.
 *
 * <p>The tests share one database. Lerato's figures are only read, never changed, so tests can check exact values for
 * her. Tests that submit new notices use Thabo's products and compare before and after values instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WithdrawalApiIntegrationTest {

    private static final String THABO = "thabo.mokoena@example.com"; // 70
    private static final String SIPHO = "sipho.ndlovu@example.com"; // exactly 65
    private static final String LERATO = "lerato.dlamini@example.com"; // 40

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvestorRepository investorRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    // ---- Staff views ----

    @Test
    void staffCanListAllInvestorsWithTheirTotals() throws Exception {
        // Sorted by surname, so Lerato Dlamini comes first. She has one paid notice and one still pending.
        mockMvc.perform(get("/api/investors").with(user(staff())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fullName").value("Lerato Dlamini"))
                .andExpect(jsonPath("$[0].productCount").value(2))
                .andExpect(jsonPath("$[0].totalBalance").value(350500.00))
                .andExpect(jsonPath("$[0].withdrawalCount").value(2))
                .andExpect(jsonPath("$[0].openNoticeCount").value(1))
                .andExpect(jsonPath("$[0].totalWithdrawn").value(5000.00))
                .andExpect(jsonPath("$[0].lastWithdrawalAt").isNotEmpty());
    }

    @Test
    void staffSeeWithdrawalNoticesFromAllClients() throws Exception {
        mockMvc.perform(get("/api/withdrawals").with(user(staff())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].investorName", hasItems("Thabo Mokoena", "Lerato Dlamini", "Sipho Ndlovu")));
    }

    @Test
    void staffCanListTheNoticesThatNeedAction() throws Exception {
        mockMvc.perform(get("/api/withdrawals")
                        .with(user(staff()))
                        .param("status", "PENDING")
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(oneOf("PENDING", "APPROVED"))))
                .andExpect(jsonPath("$[*].status", hasItems("PENDING", "APPROVED")));
    }

    @Test
    void staffDashboardSummarisesAllClients() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user(staff())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientCount").value(3))
                .andExpect(jsonPath("$.productCount").value(6))
                // Only Thabo (70) qualifies: Sipho is exactly 65 and Lerato is 40.
                .andExpect(jsonPath("$.retirementEligibleClients").value(1))
                .andExpect(jsonPath("$.amountOnHold").isNumber())
                .andExpect(jsonPath("$.assetsByProductType.length()").value(2))
                .andExpect(jsonPath("$.withdrawalsByMonth.length()").value(6));
    }

    @Test
    void investorsCannotSeeTheDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user(signedInAs(LERATO)))).andExpect(status().isForbidden());
    }

    @Test
    void investorsCannotListInvestors() throws Exception {
        mockMvc.perform(get("/api/investors").with(user(signedInAs(LERATO)))).andExpect(status().isForbidden());
    }

    // ---- Portfolios ----

    @Test
    void investorSeesOwnPortfolioWithRuleHints() throws Exception {
        // Lerato's savings: balance 40,500 with 2,000 on hold for her pending notice, so 38,500 is available and 90% of
        // that (34,650) is the most she can ask for.
        mockMvc.perform(get("/api/investors/{id}/portfolio", investor(LERATO).getId())
                        .with(user(signedInAs(LERATO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investor.fullName").value("Lerato Dlamini"))
                .andExpect(jsonPath("$.investor.age").value(40))
                .andExpect(jsonPath("$.products.length()").value(2))
                .andExpect(jsonPath("$.products[0].type").value("RETIREMENT"))
                .andExpect(jsonPath("$.products[0].withdrawalAllowed").value(false))
                .andExpect(jsonPath("$.products[0].maxWithdrawalAmount").value(0))
                .andExpect(jsonPath("$.products[1].type").value("SAVINGS"))
                .andExpect(jsonPath("$.products[1].withdrawalAllowed").value(true))
                .andExpect(jsonPath("$.products[1].balance").value(40500.00))
                .andExpect(jsonPath("$.products[1].heldAmount").value(2000.00))
                .andExpect(jsonPath("$.products[1].availableBalance").value(38500.00))
                .andExpect(jsonPath("$.products[1].maxWithdrawalAmount").value(34650.00));
    }

    @Test
    void investorCannotOpenAnotherInvestorsPortfolio() throws Exception {
        mockMvc.perform(get("/api/investors/{id}/portfolio", investor(THABO).getId())
                        .with(user(signedInAs(LERATO))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You can only access your own portfolio."));
    }

    @Test
    void staffCanOpenAnyPortfolio() throws Exception {
        mockMvc.perform(get("/api/investors/{id}/portfolio", investor(THABO).getId())
                        .with(user(staff())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investor.fullName").value("Thabo Mokoena"));
    }

    @Test
    void unknownInvestorReturns404ForStaff() throws Exception {
        mockMvc.perform(get("/api/investors/{id}/portfolio", 999_999).with(user(staff())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    // ---- Submitting notices ----

    @Test
    void submittedNoticeIsPendingAndAppearsInHistoryAndCsv() throws Exception {
        Product retirement = product(THABO, ProductType.RETIREMENT);

        mockMvc.perform(submitRequest(THABO, retirement, "1234.56"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.investorName").value("Thabo Mokoena"))
                .andExpect(jsonPath("$.amount").value(1234.56))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.balanceBefore").doesNotExist());

        mockMvc.perform(get("/api/withdrawals")
                        .with(user(signedInAs(THABO)))
                        .param("productId", retirement.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(1234.56)); // newest first

        mockMvc.perform(get("/api/withdrawals/export")
                        .with(user(signedInAs(THABO)))
                        .param("productId", retirement.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string(containsString("Thabo Mokoena,Retirement Annuity,RETIREMENT,1234.56,PENDING,,,")));
    }

    @Test
    void investorCannotWithdrawFromAnotherInvestorsProduct() throws Exception {
        Product thabosSavings = product(THABO, ProductType.SAVINGS);

        mockMvc.perform(submitRequest(LERATO, thabosSavings, "100.00")).andExpect(status().isForbidden());
    }

    @Test
    void historyIsLimitedToTheSignedInInvestor() throws Exception {
        AuthenticatedUser lerato = signedInAs(LERATO);

        mockMvc.perform(get("/api/withdrawals").with(user(lerato)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].investorName", everyItem(equalTo("Lerato Dlamini"))));

        mockMvc.perform(get("/api/withdrawals")
                        .with(user(lerato))
                        .param("investorId", investor(THABO).getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void investorAgedExactly65CannotWithdrawFromRetirementProduct() throws Exception {
        Product retirement = product(SIPHO, ProductType.RETIREMENT);

        mockMvc.perform(submitRequest(SIPHO, retirement, "100.00"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("RETIREMENT_AGE_RESTRICTION"));
    }

    @Test
    void withdrawalAbove90PercentIsRejected() throws Exception {
        Product savings = product(LERATO, ProductType.SAVINGS);

        mockMvc.perform(submitRequest(
                        LERATO, savings, savings.getAvailableBalance().toPlainString()))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("EXCEEDS_WITHDRAWAL_LIMIT"));
    }

    @Test
    void moneyOnHoldIsNotAvailableForNewNotices() throws Exception {
        // 39,000 is below Lerato's balance (40,500) but above what is available (38,500).
        Product savings = product(LERATO, ProductType.SAVINGS);

        mockMvc.perform(submitRequest(LERATO, savings, "39000.00"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.detail", containsString("R 2,000.00 is on hold for open withdrawal notices")));
    }

    // ---- The workflow ----

    @Test
    void noticeGoesFromPendingToApprovedToPaid() throws Exception {
        Product before = product(THABO, ProductType.SAVINGS);
        BigDecimal amount = new BigDecimal("1000.00");
        long id = submit(THABO, before, amount.toPlainString());

        // Submitting holds the money but leaves the balance alone.
        Product afterSubmit = reload(before);
        assertThat(afterSubmit.getBalance()).isEqualByComparingTo(before.getBalance());
        assertThat(afterSubmit.getHeldAmount())
                .isEqualByComparingTo(before.getHeldAmount().add(amount));

        // An investor cannot approve their own notice, and staff cannot pay it before approving it.
        mockMvc.perform(action(id, "approve", signedInAs(THABO))).andExpect(status().isForbidden());
        mockMvc.perform(action(id, "pay", staff()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStatus").value("PENDING"));

        mockMvc.perform(action(id, "approve", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value(DataSeeder.STAFF_USERNAME));

        mockMvc.perform(action(id, "pay", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.balanceBefore").value(before.getBalance().doubleValue()))
                .andExpect(jsonPath("$.balanceAfter")
                        .value(before.getBalance().subtract(amount).doubleValue()));

        // Paying takes the money out of the balance and off hold at the same time.
        Product afterPay = reload(before);
        assertThat(afterPay.getBalance())
                .isEqualByComparingTo(before.getBalance().subtract(amount));
        assertThat(afterPay.getHeldAmount()).isEqualByComparingTo(before.getHeldAmount());

        // Paid is final.
        mockMvc.perform(action(id, "pay", staff())).andExpect(status().isConflict());
    }

    @Test
    void onlyTheInvestorCanCancelTheirPendingNotice() throws Exception {
        Product before = product(THABO, ProductType.SAVINGS);
        long id = submit(THABO, before, "500.00");

        mockMvc.perform(action(id, "cancel", staff())).andExpect(status().isForbidden());
        mockMvc.perform(action(id, "cancel", signedInAs(LERATO))).andExpect(status().isForbidden());

        mockMvc.perform(action(id, "cancel", signedInAs(THABO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(reload(before).getHeldAmount()).isEqualByComparingTo(before.getHeldAmount());
    }

    @Test
    void rejectingNeedsAReasonThatTheInvestorCanSee() throws Exception {
        Product before = product(THABO, ProductType.SAVINGS);
        long id = submit(THABO, before, "750.00");

        mockMvc.perform(reject(id, ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").value("A reason is required"));

        mockMvc.perform(reject(id, "Please update your bank details first."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        assertThat(reload(before).getHeldAmount()).isEqualByComparingTo(before.getHeldAmount());

        mockMvc.perform(get("/api/withdrawals/{id}", id).with(user(signedInAs(THABO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejectionReason").value("Please update your bank details first."));
    }

    // ---- helpers ----

    private RequestBuilder submitRequest(String username, Product product, String amount) {
        return post("/api/withdrawals")
                .with(user(signedInAs(username)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\": %d, \"amount\": %s}".formatted(product.getId(), amount));
    }

    /** Submits a notice and returns its id. */
    private long submit(String username, Product product, String amount) throws Exception {
        MvcResult result = mockMvc.perform(submitRequest(username, product, amount))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private RequestBuilder action(long noticeId, String action, AuthenticatedUser actor) {
        return post("/api/withdrawals/{id}/" + action, noticeId)
                .with(user(actor))
                .with(csrf());
    }

    private RequestBuilder reject(long noticeId, String reason) {
        return post("/api/withdrawals/{id}/reject", noticeId)
                .with(user(staff()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"%s\"}".formatted(reason));
    }

    private AuthenticatedUser signedInAs(String username) {
        return (AuthenticatedUser) userDetailsService.loadUserByUsername(username);
    }

    private AuthenticatedUser staff() {
        return signedInAs(DataSeeder.STAFF_USERNAME);
    }

    private Investor investor(String email) {
        return investorRepository.findByEmail(email).orElseThrow();
    }

    private Product product(String email, ProductType type) {
        return productRepository.findByInvestorIdOrderByIdAsc(investor(email).getId()).stream()
                .filter(product -> product.getType() == type)
                .findFirst()
                .orElseThrow();
    }

    private Product reload(Product product) {
        return productRepository.findById(product.getId()).orElseThrow();
    }
}
