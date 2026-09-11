package com.enviro.assessment.junior.smsibi.service;

import com.enviro.assessment.junior.smsibi.dto.WithdrawalFilter;
import com.enviro.assessment.junior.smsibi.dto.WithdrawalRequest;
import com.enviro.assessment.junior.smsibi.dto.WithdrawalResponse;
import com.enviro.assessment.junior.smsibi.entity.Product;
import com.enviro.assessment.junior.smsibi.entity.WithdrawalNotice;
import com.enviro.assessment.junior.smsibi.exception.AccessForbiddenException;
import com.enviro.assessment.junior.smsibi.exception.ResourceNotFoundException;
import com.enviro.assessment.junior.smsibi.mapper.DtoMapper;
import com.enviro.assessment.junior.smsibi.repository.ProductRepository;
import com.enviro.assessment.junior.smsibi.repository.WithdrawalNoticeRepository;
import com.enviro.assessment.junior.smsibi.repository.WithdrawalSpecifications;
import com.enviro.assessment.junior.smsibi.security.AccessGuard;
import com.enviro.assessment.junior.smsibi.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for withdrawal notices: create one (with ownership, rule checks and balance calculation) and query history.
 */
@Service
public class WithdrawalService {

    private static final Logger log = LoggerFactory.getLogger(WithdrawalService.class);

    // Newest first; id breaks ties when two notices share the same timestamp.
    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final ProductRepository productRepository;
    private final WithdrawalNoticeRepository noticeRepository;
    private final WithdrawalPolicy withdrawalPolicy;
    private final Clock clock;

    public WithdrawalService(
            ProductRepository productRepository,
            WithdrawalNoticeRepository noticeRepository,
            WithdrawalPolicy withdrawalPolicy,
            Clock clock) {
        this.productRepository = productRepository;
        this.noticeRepository = noticeRepository;
        this.withdrawalPolicy = withdrawalPolicy;
        this.clock = clock;
    }

    /**
     * Validates and records a withdrawal, and deducts it from the product balance. Only the investor who owns the
     * product may withdraw; staff accounts are read-only.
     *
     * <p>{@code @Transactional}: the balance update and the notice insert either both succeed or both roll back, so a
     * notice can never exist without its balance being deducted (or the other way round). The product's balance
     * change is saved automatically at commit, because it is a managed entity.
     */
    @Transactional
    public WithdrawalResponse createWithdrawal(WithdrawalRequest request, AuthenticatedUser user) {
        if (user.isAdmin()) {
            throw new AccessForbiddenException("Staff accounts are read-only and cannot submit withdrawals.");
        }
        Product product = productRepository
                .findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));
        AccessGuard.requireInvestorAccess(user, product.getInvestor().getId());

        // Normalise to cents, so the stored amount and the rule checks work with exactly the same value.
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        withdrawalPolicy.validate(product, amount, LocalDate.now(clock));

        BigDecimal balanceBefore = product.getBalance();
        product.withdraw(amount);

        // Truncated to seconds: statements do not need sub-second precision, and the value returned now then matches
        // what is read back from the database later.
        LocalDateTime createdAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        WithdrawalNotice notice = noticeRepository.save(
                new WithdrawalNotice(product, amount, balanceBefore, product.getBalance(), createdAt));

        // Audit trail: who withdrew what (never log passwords or session ids).
        log.info(
                "Withdrawal notice {} created by {}: {} from product {}",
                notice.getId(),
                user.getUsername(),
                amount,
                product.getId());
        return DtoMapper.toWithdrawalResponse(notice);
    }

    @Transactional(readOnly = true)
    public WithdrawalResponse getWithdrawal(Long id, AuthenticatedUser user) {
        WithdrawalNotice notice = noticeRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal notice", id));
        AccessGuard.requireInvestorAccess(
                user, notice.getProduct().getInvestor().getId());
        return DtoMapper.toWithdrawalResponse(notice);
    }

    /** Investors only ever get their own withdrawals, whatever filter they send (see AccessGuard). */
    @Transactional(readOnly = true)
    public List<WithdrawalResponse> findWithdrawals(WithdrawalFilter filter, AuthenticatedUser user) {
        WithdrawalFilter scoped = AccessGuard.restrictToUser(filter, user);
        return noticeRepository.findAll(WithdrawalSpecifications.matching(scoped), NEWEST_FIRST).stream()
                .map(DtoMapper::toWithdrawalResponse)
                .toList();
    }
}
