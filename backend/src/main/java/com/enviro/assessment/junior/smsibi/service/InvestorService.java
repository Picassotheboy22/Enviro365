package com.enviro.assessment.junior.smsibi.service;

import com.enviro.assessment.junior.smsibi.dto.InvestorSummary;
import com.enviro.assessment.junior.smsibi.dto.PortfolioResponse;
import com.enviro.assessment.junior.smsibi.dto.ProductResponse;
import com.enviro.assessment.junior.smsibi.entity.Investor;
import com.enviro.assessment.junior.smsibi.exception.AccessForbiddenException;
import com.enviro.assessment.junior.smsibi.exception.ResourceNotFoundException;
import com.enviro.assessment.junior.smsibi.mapper.DtoMapper;
import com.enviro.assessment.junior.smsibi.repository.InvestorProductTotals;
import com.enviro.assessment.junior.smsibi.repository.InvestorRepository;
import com.enviro.assessment.junior.smsibi.repository.InvestorWithdrawalTotals;
import com.enviro.assessment.junior.smsibi.repository.ProductRepository;
import com.enviro.assessment.junior.smsibi.repository.WithdrawalNoticeRepository;
import com.enviro.assessment.junior.smsibi.security.AccessGuard;
import com.enviro.assessment.junior.smsibi.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only use cases for investors and their portfolios.
 *
 * <p>{@code readOnly = true} tells Hibernate it can skip change tracking (dirty checking) for these queries. Mapping to
 * DTOs happens inside the transaction, so lazy relationships can still be loaded (open-in-view is disabled).
 */
@Service
@Transactional(readOnly = true)
public class InvestorService {

    private final InvestorRepository investorRepository;
    private final ProductRepository productRepository;
    private final WithdrawalNoticeRepository noticeRepository;
    private final WithdrawalPolicy withdrawalPolicy;
    private final Clock clock;

    // Constructor injection (rather than @Autowired fields): dependencies are explicit and final,
    // and unit tests can simply call "new InvestorService(...)" with mocks.
    public InvestorService(
            InvestorRepository investorRepository,
            ProductRepository productRepository,
            WithdrawalNoticeRepository noticeRepository,
            WithdrawalPolicy withdrawalPolicy,
            Clock clock) {
        this.investorRepository = investorRepository;
        this.productRepository = productRepository;
        this.noticeRepository = noticeRepository;
        this.withdrawalPolicy = withdrawalPolicy;
        this.clock = clock;
    }

    /**
     * The staff "Clients" overview: every investor with their totals.
     *
     * <p>Staff only. SecurityConfig already restricts the URL to ADMIN; checking again here keeps the service safe even
     * if it is called from somewhere else later ("defence in depth").
     *
     * <p>The totals come from two GROUP BY queries covering all investors at once. Loading each portfolio instead would
     * cost one query per investor (the "N+1 problem") and get slower as the client list grows.
     */
    public List<InvestorSummary> listInvestors(AuthenticatedUser user) {
        if (!user.isAdmin()) {
            throw new AccessForbiddenException("Only Enviro365 staff can list investors.");
        }
        LocalDate today = LocalDate.now(clock);
        Map<Long, InvestorProductTotals> productTotals = productRepository.totalsPerInvestor().stream()
                .collect(Collectors.toMap(InvestorProductTotals::getInvestorId, Function.identity()));
        Map<Long, InvestorWithdrawalTotals> withdrawalTotals = noticeRepository.totalsPerInvestor().stream()
                .collect(Collectors.toMap(InvestorWithdrawalTotals::getInvestorId, Function.identity()));

        return investorRepository.findAllByOrderByLastNameAscFirstNameAsc().stream()
                .map(investor -> DtoMapper.toSummary(
                        investor, today, productTotals.get(investor.getId()), withdrawalTotals.get(investor.getId())))
                .toList();
    }

    public PortfolioResponse getPortfolio(Long investorId, AuthenticatedUser user) {
        // Checked before the database lookup, so an investor cannot even find out which other ids exist.
        AccessGuard.requireInvestorAccess(user, investorId);

        Investor investor = investorRepository
                .findById(investorId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor", investorId));
        LocalDate today = LocalDate.now(clock);

        List<ProductResponse> products = productRepository.findByInvestorIdOrderByIdAsc(investorId).stream()
                .map(product -> DtoMapper.toProductResponse(
                        product,
                        withdrawalPolicy.maxWithdrawalAmount(product, today),
                        withdrawalPolicy.restrictionFor(product, today).orElse(null)))
                .toList();

        BigDecimal totalBalance =
                products.stream().map(ProductResponse::balance).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        return new PortfolioResponse(DtoMapper.toDetails(investor, today), products, totalBalance);
    }
}
