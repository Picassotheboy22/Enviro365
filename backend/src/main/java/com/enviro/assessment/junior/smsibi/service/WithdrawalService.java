package com.enviro.assessment.junior.smsibi.service;

import com.enviro.assessment.junior.smsibi.dto.WithdrawalFilter;
import com.enviro.assessment.junior.smsibi.dto.WithdrawalRequest;
import com.enviro.assessment.junior.smsibi.dto.WithdrawalResponse;
import com.enviro.assessment.junior.smsibi.entity.NoticeStatus;
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
 * Use cases for withdrawal notices: submit one, move it through the workflow, and query history.
 *
 * <p>The workflow: an investor submits a notice (PENDING, and the amount is put on hold), staff approve it (APPROVED)
 * and then mark it as paid (PAID, and the amount leaves the balance). Staff can reject a pending notice with a reason,
 * and the investor can cancel it while it is still pending. Both release the hold. {@link NoticeStatus} defines which
 * moves exist; this class decides who may make each one.
 *
 * <p>Every method that changes data is {@code @Transactional}: a notice and its product change together or not at
 * all, so money can never be held without a notice (or the other way round). Changes to managed entities are saved
 * automatically when the transaction commits.
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
     * Validates a new notice and submits it as PENDING, putting the amount on hold. Only the investor who owns the
     * product may do this. Nothing is deducted from the balance until staff pay the notice.
     */
    @Transactional
    public WithdrawalResponse createWithdrawal(WithdrawalRequest request, AuthenticatedUser user) {
        if (user.isAdmin()) {
            throw new AccessForbiddenException(
                    "Staff accounts cannot submit withdrawals. Only the investor who owns a product can.");
        }
        Product product = productRepository
                .findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));
        AccessGuard.requireInvestorAccess(user, product.getInvestor().getId());

        // Normalise to cents, so the stored amount and the rule checks work with exactly the same value.
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        withdrawalPolicy.validate(product, amount, LocalDate.now(clock));

        WithdrawalNotice notice = noticeRepository.save(WithdrawalNotice.submit(product, amount, now()));
        audit(notice, "submitted", user);
        return DtoMapper.toWithdrawalResponse(notice);
    }

    /**
     * Staff approve a pending notice. The rules are not checked again: they were checked when the notice was
     * submitted, and the money has been on hold ever since, so it is still there. Age only goes up, so an investor who
     * was old enough for a retirement withdrawal still is.
     */
    @Transactional
    public WithdrawalResponse approve(Long noticeId, AuthenticatedUser user) {
        requireStaff(user);
        WithdrawalNotice notice = findNotice(noticeId);
        notice.approve(user.getUsername(), now());
        audit(notice, "approved", user);
        return DtoMapper.toWithdrawalResponse(notice);
    }

    /** Staff turn down a pending notice. The investor is shown the reason, and the held amount is released. */
    @Transactional
    public WithdrawalResponse reject(Long noticeId, String reason, AuthenticatedUser user) {
        requireStaff(user);
        WithdrawalNotice notice = findNotice(noticeId);
        notice.reject(user.getUsername(), now(), reason.strip());
        audit(notice, "rejected", user);
        return DtoMapper.toWithdrawalResponse(notice);
    }

    /** Staff record that an approved notice has been paid. Only now does the amount leave the product balance. */
    @Transactional
    public WithdrawalResponse pay(Long noticeId, AuthenticatedUser user) {
        requireStaff(user);
        WithdrawalNotice notice = findNotice(noticeId);
        notice.markPaid(user.getUsername(), now());
        audit(notice, "paid", user);
        return DtoMapper.toWithdrawalResponse(notice);
    }

    /** The investor withdraws their own notice while it is still pending. The held amount is released. */
    @Transactional
    public WithdrawalResponse cancel(Long noticeId, AuthenticatedUser user) {
        if (user.isAdmin()) {
            throw new AccessForbiddenException(
                    "Only the investor who submitted a notice can cancel it. Staff can reject it instead.");
        }
        WithdrawalNotice notice = findNotice(noticeId);
        AccessGuard.requireInvestorAccess(
                user, notice.getProduct().getInvestor().getId());
        notice.cancel(now());
        audit(notice, "cancelled", user);
        return DtoMapper.toWithdrawalResponse(notice);
    }

    @Transactional(readOnly = true)
    public WithdrawalResponse getWithdrawal(Long id, AuthenticatedUser user) {
        WithdrawalNotice notice = findNotice(id);
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

    // SecurityConfig already limits these URLs to staff; checked again here as defence in depth.
    private static void requireStaff(AuthenticatedUser user) {
        if (!user.isAdmin()) {
            throw new AccessForbiddenException("Only Enviro365 staff can review and pay withdrawal notices.");
        }
    }

    private WithdrawalNotice findNotice(Long id) {
        return noticeRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal notice", id));
    }

    // Truncated to seconds: statements do not need sub-second precision, and the value returned now then matches what
    // is read back from the database later.
    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    // Audit trail: who did what to which notice. Never log passwords, session ids or free text typed by users.
    private static void audit(WithdrawalNotice notice, String action, AuthenticatedUser user) {
        log.info(
                "Withdrawal notice {} {} by {}: {} from product {}",
                notice.getId(),
                action,
                user.getUsername(),
                notice.getAmount(),
                notice.getProduct().getId());
    }
}
