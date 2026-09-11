package com.enviro.assessment.junior.smsibi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.enviro.assessment.junior.smsibi.dto.InvestorSummary;
import com.enviro.assessment.junior.smsibi.entity.Investor;
import com.enviro.assessment.junior.smsibi.exception.AccessForbiddenException;
import com.enviro.assessment.junior.smsibi.repository.InvestorProductTotals;
import com.enviro.assessment.junior.smsibi.repository.InvestorRepository;
import com.enviro.assessment.junior.smsibi.repository.InvestorWithdrawalTotals;
import com.enviro.assessment.junior.smsibi.repository.ProductRepository;
import com.enviro.assessment.junior.smsibi.repository.WithdrawalNoticeRepository;
import com.enviro.assessment.junior.smsibi.security.TestUsers;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests the staff "Clients" overview: combining investors with their product and withdrawal totals. */
@ExtendWith(MockitoExtension.class)
class InvestorServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-10T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WithdrawalNoticeRepository noticeRepository;

    private InvestorService service;

    @BeforeEach
    void setUp() {
        service = new InvestorService(
                investorRepository, productRepository, noticeRepository, new WithdrawalPolicy(), CLOCK);
    }

    @Test
    void clientsOverviewCombinesEachClientsTotals() {
        Investor lerato = investor(3L, "Lerato", "Dlamini", LocalDate.of(1986, 2, 10));
        Investor thabo = investor(1L, "Thabo", "Mokoena", LocalDate.of(1956, 5, 10));
        LocalDateTime lastWithdrawal = LocalDateTime.of(2026, 9, 3, 11, 0);
        when(investorRepository.findAllByOrderByLastNameAscFirstNameAsc()).thenReturn(List.of(lerato, thabo));
        when(productRepository.totalsPerInvestor())
                .thenReturn(List.of(
                        new ProductTotals(1L, 2L, new BigDecimal("920000.00")),
                        new ProductTotals(3L, 2L, new BigDecimal("350500.00"))));
        // Only Thabo has withdrawn.
        when(noticeRepository.totalsPerInvestor())
                .thenReturn(List.of(new WithdrawalTotals(1L, 2L, new BigDecimal("40000.00"), lastWithdrawal)));

        List<InvestorSummary> summaries = service.listInvestors(TestUsers.admin());

        assertThat(summaries).extracting(InvestorSummary::fullName).containsExactly("Lerato Dlamini", "Thabo Mokoena");

        InvestorSummary thaboSummary = summaries.get(1);
        assertThat(thaboSummary.age()).isEqualTo(70);
        assertThat(thaboSummary.productCount()).isEqualTo(2);
        assertThat(thaboSummary.totalBalance()).isEqualByComparingTo("920000.00");
        assertThat(thaboSummary.withdrawalCount()).isEqualTo(2);
        assertThat(thaboSummary.totalWithdrawn()).isEqualByComparingTo("40000.00");
        assertThat(thaboSummary.lastWithdrawalAt()).isEqualTo(lastWithdrawal);

        // A client with no withdrawals gets zeros rather than nulls, and no last date.
        InvestorSummary leratoSummary = summaries.get(0);
        assertThat(leratoSummary.withdrawalCount()).isZero();
        assertThat(leratoSummary.totalWithdrawn()).isEqualByComparingTo("0.00");
        assertThat(leratoSummary.lastWithdrawalAt()).isNull();
    }

    @Test
    void investorsCannotSeeTheClientList() {
        assertThatThrownBy(() -> service.listInvestors(TestUsers.investor(7L)))
                .isInstanceOf(AccessForbiddenException.class);

        verifyNoInteractions(investorRepository, productRepository, noticeRepository);
    }

    private static Investor investor(long id, String firstName, String lastName, LocalDate dateOfBirth) {
        Investor investor = new Investor(
                firstName, lastName, firstName.toLowerCase(Locale.ROOT) + "@example.com", null, dateOfBirth);
        ReflectionTestUtils.setField(investor, "id", id); // normally assigned by the database
        return investor;
    }

    // Stand-ins for the rows the aggregate queries return. A record component called "getX" generates a getX()
    // accessor, which is exactly the method the projection interface asks for.
    private record ProductTotals(Long getInvestorId, Long getProductCount, BigDecimal getTotalBalance)
            implements InvestorProductTotals {}

    private record WithdrawalTotals(
            Long getInvestorId,
            Long getWithdrawalCount,
            BigDecimal getTotalWithdrawn,
            LocalDateTime getLastWithdrawalAt)
            implements InvestorWithdrawalTotals {}
}
