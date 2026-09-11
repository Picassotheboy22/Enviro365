package com.enviro.assessment.junior.smsibi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.enviro.assessment.junior.smsibi.dto.DashboardResponse;
import com.enviro.assessment.junior.smsibi.dto.DashboardResponse.MonthlyWithdrawals;
import com.enviro.assessment.junior.smsibi.entity.NoticeStatus;
import com.enviro.assessment.junior.smsibi.entity.ProductType;
import com.enviro.assessment.junior.smsibi.exception.AccessForbiddenException;
import com.enviro.assessment.junior.smsibi.repository.InvestorRepository;
import com.enviro.assessment.junior.smsibi.repository.PaidAmount;
import com.enviro.assessment.junior.smsibi.repository.ProductRepository;
import com.enviro.assessment.junior.smsibi.repository.ProductTypeTotals;
import com.enviro.assessment.junior.smsibi.repository.StatusTotals;
import com.enviro.assessment.junior.smsibi.repository.WithdrawalNoticeRepository;
import com.enviro.assessment.junior.smsibi.security.TestUsers;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    // "Now" is 10 Sep 2026 10:00: the chart covers April to September 2026, and "the last 30 days" starts 11 Aug 10:00.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-10T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WithdrawalNoticeRepository noticeRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(investorRepository, productRepository, noticeRepository, CLOCK);
    }

    @Test
    void combinesTheTotalsIntoTheDashboard() {
        when(investorRepository.count()).thenReturn(3L);
        // Anyone born on or before 10 Sep 1960 is at least 66, i.e. older than 65.
        when(investorRepository.countByDateOfBirthLessThanEqual(LocalDate.of(1960, 9, 10)))
                .thenReturn(1L);
        when(productRepository.totalsPerProductType())
                .thenReturn(List.of(
                        new TypeTotals(ProductType.RETIREMENT, 3L, new BigDecimal("1650000.00")),
                        new TypeTotals(ProductType.SAVINGS, 3L, new BigDecimal("208000.00"))));
        when(noticeRepository.totalsPerStatus())
                .thenReturn(List.of(
                        new Totals(NoticeStatus.PAID, 4L, new BigDecimal("60000.00")),
                        new Totals(NoticeStatus.PENDING, 2L, new BigDecimal("3000.00")),
                        new Totals(NoticeStatus.APPROVED, 1L, new BigDecimal("15000.00")),
                        new Totals(NoticeStatus.REJECTED, 1L, new BigDecimal("3000.00"))));
        when(noticeRepository.countByCreatedAtGreaterThanEqual(LocalDateTime.of(2026, 8, 11, 10, 0)))
                .thenReturn(5L);
        // The query must start at the beginning of the chart window: 1 April 2026.
        when(noticeRepository.paidSince(LocalDateTime.of(2026, 4, 1, 0, 0)))
                .thenReturn(List.of(
                        payment("2026-05-02T09:00", "20000.00"),
                        payment("2026-08-05T09:00", "25000.00"), // more than 30 days ago
                        payment("2026-08-20T09:00", "10000.00"),
                        payment("2026-09-05T09:00", "5000.00")));

        DashboardResponse dashboard = service.getDashboard(TestUsers.admin());

        assertThat(dashboard.clientCount()).isEqualTo(3);
        assertThat(dashboard.retirementEligibleClients()).isEqualTo(1);
        assertThat(dashboard.productCount()).isEqualTo(6);
        assertThat(dashboard.assetsUnderManagement()).isEqualByComparingTo("1858000.00");

        // The workflow: every status counts as a notice, open ones hold money, only paid ones count as withdrawn.
        assertThat(dashboard.noticeCount()).isEqualTo(8);
        assertThat(dashboard.awaitingApproval()).isEqualTo(2);
        assertThat(dashboard.awaitingPayment()).isEqualTo(1);
        assertThat(dashboard.amountOnHold()).isEqualByComparingTo("18000.00");
        assertThat(dashboard.paidCount()).isEqualTo(4);
        assertThat(dashboard.totalWithdrawn()).isEqualByComparingTo("60000.00");
        assertThat(dashboard.averageWithdrawal()).isEqualByComparingTo("15000.00");

        assertThat(dashboard.noticesLast30Days()).isEqualTo(5);
        assertThat(dashboard.withdrawnLast30Days()).isEqualByComparingTo("15000.00");

        assertThat(dashboard.withdrawalsByMonth())
                .extracting(MonthlyWithdrawals::month)
                .containsExactly("2026-04", "2026-05", "2026-06", "2026-07", "2026-08", "2026-09");
        assertThat(dashboard.withdrawalsByMonth())
                .extracting(MonthlyWithdrawals::noticeCount)
                .containsExactly(0L, 1L, 0L, 0L, 2L, 1L);
        assertThat(dashboard.withdrawalsByMonth().get(4).amount()).isEqualByComparingTo("35000.00");
    }

    @Test
    void worksWhenThereAreNoNoticesYet() {
        when(investorRepository.count()).thenReturn(0L);
        when(investorRepository.countByDateOfBirthLessThanEqual(any())).thenReturn(0L);
        when(productRepository.totalsPerProductType()).thenReturn(List.of());
        when(noticeRepository.totalsPerStatus()).thenReturn(List.of()); // no rows at all, not rows of zeros
        when(noticeRepository.countByCreatedAtGreaterThanEqual(any())).thenReturn(0L);
        when(noticeRepository.paidSince(any())).thenReturn(List.of());

        DashboardResponse dashboard = service.getDashboard(TestUsers.admin());

        assertThat(dashboard.assetsUnderManagement()).isEqualByComparingTo("0.00");
        assertThat(dashboard.noticeCount()).isZero();
        assertThat(dashboard.awaitingApproval()).isZero();
        assertThat(dashboard.amountOnHold()).isEqualByComparingTo("0.00");
        assertThat(dashboard.totalWithdrawn()).isEqualByComparingTo("0.00");
        assertThat(dashboard.averageWithdrawal()).isEqualByComparingTo("0.00"); // no division by zero
        assertThat(dashboard.withdrawalsByMonth()).hasSize(6).allSatisfy(month -> assertThat(month.noticeCount())
                .isZero());
    }

    @Test
    void investorsCannotSeeTheDashboard() {
        assertThatThrownBy(() -> service.getDashboard(TestUsers.investor(7L)))
                .isInstanceOf(AccessForbiddenException.class);

        verifyNoInteractions(investorRepository, productRepository, noticeRepository);
    }

    private static PaidAmount payment(String paidAt, String amount) {
        return new Payment(LocalDateTime.parse(paidAt), new BigDecimal(amount));
    }

    // Stand-ins for the rows the aggregate queries return. A record component called "getX" generates a getX()
    // accessor, which is exactly the method the projection interface asks for.
    private record TypeTotals(ProductType getType, Long getProductCount, BigDecimal getTotalBalance)
            implements ProductTypeTotals {}

    private record Totals(NoticeStatus getStatus, Long getNoticeCount, BigDecimal getTotalAmount)
            implements StatusTotals {}

    private record Payment(LocalDateTime getPaidAt, BigDecimal getAmount) implements PaidAmount {}
}
