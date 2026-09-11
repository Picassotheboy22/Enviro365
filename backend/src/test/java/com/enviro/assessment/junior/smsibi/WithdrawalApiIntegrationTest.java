package com.enviro.assessment.junior.smsibi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end tests through every layer (security, HTTP, controller, service, JPA, H2) using DataSeeder's demo data.
 * Each request runs as a real seeded user, loaded the same way Spring Security loads it at sign-in.
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

    @Test
    void staffCanListAllInvestorsWithTheirTotals() throws Exception {
        // Sorted by surname, so Lerato Dlamini comes first. Her figures are not changed by any other test.
        mockMvc.perform(get("/api/investors").with(user(signedInAs(DataSeeder.STAFF_USERNAME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fullName").value("Lerato Dlamini"))
                .andExpect(jsonPath("$[0].productCount").value(2))
                .andExpect(jsonPath("$[0].totalBalance").value(350500.00))
                .andExpect(jsonPath("$[0].withdrawalCount").value(1))
                .andExpect(jsonPath("$[0].totalWithdrawn").value(5000.00))
                .andExpect(jsonPath("$[0].lastWithdrawalAt").isNotEmpty());
    }

    @Test
    void staffSeeWithdrawalNoticesFromAllClients() throws Exception {
        mockMvc.perform(get("/api/withdrawals").with(user(signedInAs(DataSeeder.STAFF_USERNAME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].investorName", hasItems("Thabo Mokoena", "Lerato Dlamini", "Sipho Ndlovu")));
    }

    @Test
    void staffDashboardSummarisesAllClients() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user(signedInAs(DataSeeder.STAFF_USERNAME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientCount").value(3))
                .andExpect(jsonPath("$.productCount").value(6))
                // Only Thabo (70) qualifies: Sipho is exactly 65 and Lerato is 40.
                .andExpect(jsonPath("$.retirementEligibleClients").value(1))
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

    @Test
    void investorSeesOwnPortfolioWithRuleHints() throws Exception {
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
                .andExpect(jsonPath("$.products[1].withdrawalAllowed").value(true));
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
                        .with(user(signedInAs(DataSeeder.STAFF_USERNAME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investor.fullName").value("Thabo Mokoena"));
    }

    @Test
    void eligibleWithdrawalIsSavedAndAppearsInHistoryAndCsv() throws Exception {
        Product retirement = product(THABO, ProductType.RETIREMENT);
        AuthenticatedUser thabo = signedInAs(THABO);

        mockMvc.perform(post("/api/withdrawals")
                        .with(user(thabo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": %d, \"amount\": 1234.56}".formatted(retirement.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.investorName").value("Thabo Mokoena"))
                .andExpect(jsonPath("$.amount").value(1234.56));

        mockMvc.perform(get("/api/withdrawals")
                        .with(user(thabo))
                        .param("productId", retirement.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(1234.56)); // newest first

        mockMvc.perform(get("/api/withdrawals/export")
                        .with(user(thabo))
                        .param("productId", retirement.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thabo Mokoena,Retirement Annuity,RETIREMENT,1234.56")));
    }

    @Test
    void investorCannotWithdrawFromAnotherInvestorsProduct() throws Exception {
        Product thabosSavings = product(THABO, ProductType.SAVINGS);

        mockMvc.perform(post("/api/withdrawals")
                        .with(user(signedInAs(LERATO)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": %d, \"amount\": 100.00}".formatted(thabosSavings.getId())))
                .andExpect(status().isForbidden());
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

        mockMvc.perform(post("/api/withdrawals")
                        .with(user(signedInAs(SIPHO)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": %d, \"amount\": 100.00}".formatted(retirement.getId())))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("RETIREMENT_AGE_RESTRICTION"));
    }

    @Test
    void withdrawalAbove90PercentIsRejected() throws Exception {
        Product savings = product(LERATO, ProductType.SAVINGS);

        mockMvc.perform(post("/api/withdrawals")
                        .with(user(signedInAs(LERATO)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"productId\": %d, \"amount\": %s}".formatted(savings.getId(), savings.getBalance())))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("EXCEEDS_WITHDRAWAL_LIMIT"));
    }

    @Test
    void unknownInvestorReturns404ForStaff() throws Exception {
        mockMvc.perform(get("/api/investors/{id}/portfolio", 999_999).with(user(signedInAs(DataSeeder.STAFF_USERNAME))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    private AuthenticatedUser signedInAs(String username) {
        return (AuthenticatedUser) userDetailsService.loadUserByUsername(username);
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
}
