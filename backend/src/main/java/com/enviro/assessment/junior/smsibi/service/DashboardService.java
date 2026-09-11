package com.enviro.assessment.junior.smsibi.service;

import com.enviro.assessment.junior.smsibi.dto.DashboardResponse;
import com.enviro.assessment.junior.smsibi.dto.DashboardResponse.MonthlyWithdrawals;
import com.enviro.assessment.junior.smsibi.dto.DashboardResponse.ProductTypeTotal;
import com.enviro.assessment.junior.smsibi.entity.NoticeStatus;
import com.enviro.assessment.junior.smsibi.exception.AccessForbiddenException;
import com.enviro.assessment.junior.smsibi.repository.InvestorRepository;
import com.enviro.assessment.junior.smsibi.repository.PaidAmount;
import com.enviro.assessment.junior.smsibi.repository.ProductRepository;
import com.enviro.assessment.junior.smsibi.repository.StatusTotals;
import com.enviro.assessment.junior.smsibi.repository.WithdrawalNoticeRepository;
import com.enviro.assessment.junior.smsibi.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statistics for the staff dashboard. The counting and adding up is done by the database with aggregate queries; this
 * class only combines the results.
 *
 * <p>"Withdrawn" always means paid out. Pending and approved notices are reported separately (as waiting for staff and
 * as money on hold), because that money has not left the products yet.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** How many calendar months the "paid out per month" chart shows, including the current month. */
    static final int MONTHS_SHOWN = 6;

    /** The "recent activity" window. */
    static final int RECENT_DAYS = 30;

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final InvestorRepository investorRepository;
    private final ProductRepository productRepository;
    private final WithdrawalNoticeRepository noticeRepository;
    private final Clock clock;

    public DashboardService(
            InvestorRepository investorRepository,
            ProductRepository productRepository,
            WithdrawalNoticeRepository noticeRepository,
            Clock clock) {
        this.investorRepository = investorRepository;
        this.productRepository = productRepository;
        this.noticeRepository = noticeRepository;
        this.clock = clock;
    }

    public DashboardResponse getDashboard(AuthenticatedUser user) {
        // SecurityConfig already limits the URL to staff; checked again here as defence in depth.
        if (!user.isAdmin()) {
            throw new AccessForbiddenException("Only Enviro365 staff can view the dashboard.");
        }
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        List<ProductTypeTotal> assetsByType = productRepository.totalsPerProductType().stream()
                .map(row -> new ProductTypeTotal(row.getType(), row.getProductCount(), row.getTotalBalance()))
                .toList();
        long productCount =
                assetsByType.stream().mapToLong(ProductTypeTotal::productCount).sum();
        BigDecimal assetsUnderManagement =
                assetsByType.stream().map(ProductTypeTotal::balance).reduce(ZERO, BigDecimal::add);

        // One small row per status, so the counts and amounts for the whole workflow come from a single query.
        Map<NoticeStatus, StatusTotals> byStatus = noticeRepository.totalsPerStatus().stream()
                .collect(Collectors.toMap(StatusTotals::getStatus, Function.identity()));
        long noticeCount = byStatus.values().stream()
                .mapToLong(StatusTotals::getNoticeCount)
                .sum();
        BigDecimal amountOnHold = byStatus.values().stream()
                .filter(row -> row.getStatus().isOpen())
                .map(StatusTotals::getTotalAmount)
                .reduce(ZERO, BigDecimal::add);
        long paidCount = countOf(byStatus, NoticeStatus.PAID);
        BigDecimal totalWithdrawn = amountOf(byStatus, NoticeStatus.PAID);
        BigDecimal averageWithdrawal =
                paidCount == 0 ? ZERO : totalWithdrawn.divide(BigDecimal.valueOf(paidCount), 2, RoundingMode.HALF_UP);

        // One query fetches the payments for the chart window (the last six calendar months). That window always
        // includes the last 30 days as well, so both figures are worked out from the same rows.
        YearMonth firstMonth = YearMonth.from(today).minusMonths(MONTHS_SHOWN - 1L);
        LocalDateTime recentStart = now.minusDays(RECENT_DAYS);
        LocalDateTime windowStart = firstMonth.atDay(1).atStartOfDay();
        List<PaidAmount> windowPayments =
                noticeRepository.paidSince(windowStart.isBefore(recentStart) ? windowStart : recentStart);
        List<PaidAmount> paidLastThirtyDays = windowPayments.stream()
                .filter(payment -> !payment.getPaidAt().isBefore(recentStart))
                .toList();

        long retirementEligibleClients = investorRepository.countByDateOfBirthLessThanEqual(
                WithdrawalPolicy.latestRetirementEligibleBirthDate(today));

        return new DashboardResponse(
                investorRepository.count(),
                productCount,
                assetsUnderManagement,
                retirementEligibleClients,
                noticeCount,
                countOf(byStatus, NoticeStatus.PENDING),
                countOf(byStatus, NoticeStatus.APPROVED),
                amountOnHold,
                paidCount,
                totalWithdrawn,
                averageWithdrawal,
                noticeRepository.countByCreatedAtGreaterThanEqual(recentStart),
                sumOf(paidLastThirtyDays),
                assetsByType,
                monthlyTotals(windowPayments, firstMonth));
    }

    /** One entry per month, oldest first. Months without payments are included as zeros, so the chart has no gaps. */
    private static List<MonthlyWithdrawals> monthlyTotals(List<PaidAmount> payments, YearMonth firstMonth) {
        Map<YearMonth, List<PaidAmount>> byMonth =
                payments.stream().collect(Collectors.groupingBy(payment -> YearMonth.from(payment.getPaidAt())));
        return IntStream.range(0, MONTHS_SHOWN)
                .mapToObj(firstMonth::plusMonths)
                .map(month -> {
                    List<PaidAmount> inMonth = byMonth.getOrDefault(month, List.of());
                    return new MonthlyWithdrawals(month.toString(), inMonth.size(), sumOf(inMonth));
                })
                .toList();
    }

    // A status with no notices has no row at all, so it counts as zero.
    private static long countOf(Map<NoticeStatus, StatusTotals> byStatus, NoticeStatus status) {
        StatusTotals row = byStatus.get(status);
        return row == null ? 0 : row.getNoticeCount();
    }

    private static BigDecimal amountOf(Map<NoticeStatus, StatusTotals> byStatus, NoticeStatus status) {
        StatusTotals row = byStatus.get(status);
        return row == null ? ZERO : row.getTotalAmount();
    }

    private static BigDecimal sumOf(List<PaidAmount> payments) {
        return payments.stream().map(PaidAmount::getAmount).reduce(ZERO, BigDecimal::add);
    }
}
