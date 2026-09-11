package com.enviro.assessment.junior.smsibi.service;

import com.enviro.assessment.junior.smsibi.dto.DashboardResponse;
import com.enviro.assessment.junior.smsibi.dto.DashboardResponse.MonthlyWithdrawals;
import com.enviro.assessment.junior.smsibi.dto.DashboardResponse.ProductTypeTotal;
import com.enviro.assessment.junior.smsibi.exception.AccessForbiddenException;
import com.enviro.assessment.junior.smsibi.repository.InvestorRepository;
import com.enviro.assessment.junior.smsibi.repository.NoticeAmount;
import com.enviro.assessment.junior.smsibi.repository.NoticeTotals;
import com.enviro.assessment.junior.smsibi.repository.ProductRepository;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statistics for the staff dashboard. The counting and adding up is done by the database with aggregate queries; this
 * class only combines the results.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** How many calendar months the "withdrawals per month" chart shows, including the current month. */
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

        NoticeTotals totals = noticeRepository.overallTotals();
        long noticeCount = totals.getNoticeCount();
        BigDecimal totalWithdrawn = totals.getTotalAmount() == null ? ZERO : totals.getTotalAmount();
        BigDecimal averageWithdrawal = noticeCount == 0
                ? ZERO
                : totalWithdrawn.divide(BigDecimal.valueOf(noticeCount), 2, RoundingMode.HALF_UP);

        // One query fetches the notices for the chart window (the last six calendar months). That window always
        // includes the last 30 days as well, so both figures are worked out from the same rows.
        YearMonth firstMonth = YearMonth.from(today).minusMonths(MONTHS_SHOWN - 1L);
        LocalDateTime recentStart = now.minusDays(RECENT_DAYS);
        LocalDateTime windowStart = firstMonth.atDay(1).atStartOfDay();
        List<NoticeAmount> windowNotices =
                noticeRepository.amountsSince(windowStart.isBefore(recentStart) ? windowStart : recentStart);
        List<NoticeAmount> lastThirtyDays = windowNotices.stream()
                .filter(notice -> !notice.getCreatedAt().isBefore(recentStart))
                .toList();

        long retirementEligibleClients = investorRepository.countByDateOfBirthLessThanEqual(
                WithdrawalPolicy.latestRetirementEligibleBirthDate(today));

        return new DashboardResponse(
                investorRepository.count(),
                productCount,
                assetsUnderManagement,
                retirementEligibleClients,
                noticeCount,
                totalWithdrawn,
                averageWithdrawal,
                lastThirtyDays.size(),
                sumOf(lastThirtyDays),
                assetsByType,
                monthlyTotals(windowNotices, firstMonth));
    }

    /** One entry per month, oldest first. Months without notices are included as zeros, so the chart has no gaps. */
    private static List<MonthlyWithdrawals> monthlyTotals(List<NoticeAmount> notices, YearMonth firstMonth) {
        Map<YearMonth, List<NoticeAmount>> byMonth =
                notices.stream().collect(Collectors.groupingBy(notice -> YearMonth.from(notice.getCreatedAt())));
        return IntStream.range(0, MONTHS_SHOWN)
                .mapToObj(firstMonth::plusMonths)
                .map(month -> {
                    List<NoticeAmount> inMonth = byMonth.getOrDefault(month, List.of());
                    return new MonthlyWithdrawals(month.toString(), inMonth.size(), sumOf(inMonth));
                })
                .toList();
    }

    private static BigDecimal sumOf(List<NoticeAmount> notices) {
        return notices.stream().map(NoticeAmount::getAmount).reduce(ZERO, BigDecimal::add);
    }
}
