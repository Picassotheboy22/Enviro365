package com.enviro.assessment.junior.smsibi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.enviro.assessment.junior.smsibi.dto.WithdrawalRequest;
import com.enviro.assessment.junior.smsibi.dto.WithdrawalResponse;
import com.enviro.assessment.junior.smsibi.entity.Investor;
import com.enviro.assessment.junior.smsibi.entity.NoticeStatus;
import com.enviro.assessment.junior.smsibi.entity.Product;
import com.enviro.assessment.junior.smsibi.entity.ProductType;
import com.enviro.assessment.junior.smsibi.entity.WithdrawalNotice;
import com.enviro.assessment.junior.smsibi.exception.AccessForbiddenException;
import com.enviro.assessment.junior.smsibi.exception.BusinessRuleException;
import com.enviro.assessment.junior.smsibi.exception.InvalidStatusTransitionException;
import com.enviro.assessment.junior.smsibi.exception.ResourceNotFoundException;
import com.enviro.assessment.junior.smsibi.exception.RuleViolation;
import com.enviro.assessment.junior.smsibi.repository.ProductRepository;
import com.enviro.assessment.junior.smsibi.repository.WithdrawalNoticeRepository;
import com.enviro.assessment.junior.smsibi.security.AuthenticatedUser;
import com.enviro.assessment.junior.smsibi.security.TestUsers;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests the withdrawal use cases: submitting a notice (load, check ownership, validate, hold, save) and moving it
 * through the workflow (who may do what). The repositories are mocked, but the real WithdrawalPolicy and entities are
 * used, because the rules and the money are what we care about.
 */
@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    // A fixed clock makes "now" predictable: 10 Sep 2026, 10:00.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-10T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 10, 0);

    private static final long LERATO_ID = 7L;
    private static final AuthenticatedUser LERATO = TestUsers.investor(LERATO_ID);
    private static final AuthenticatedUser STAFF = TestUsers.admin();

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WithdrawalNoticeRepository noticeRepository;

    private WithdrawalService service;

    @BeforeEach
    void setUp() {
        service = new WithdrawalService(productRepository, noticeRepository, new WithdrawalPolicy(), CLOCK);
    }

    // ---- Submitting ----

    @Test
    void submittingCreatesAPendingNoticeAndPutsTheAmountOnHold() {
        Product product = leratosSavingsProduct("10000.00");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(noticeRepository.save(any(WithdrawalNotice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalResponse response =
                service.createWithdrawal(new WithdrawalRequest(1L, new BigDecimal("2500.00")), LERATO);

        assertThat(response.status()).isEqualTo(NoticeStatus.PENDING);
        assertThat(response.amount()).isEqualByComparingTo("2500.00");
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.investorId()).isEqualTo(LERATO_ID);
        // Nothing has been paid: the balance is unchanged and there are no balance snapshots yet.
        assertThat(response.balanceBefore()).isNull();
        assertThat(product.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(product.getHeldAmount()).isEqualByComparingTo("2500.00");

        ArgumentCaptor<WithdrawalNotice> saved = ArgumentCaptor.forClass(WithdrawalNotice.class);
        verify(noticeRepository).save(saved.capture());
        assertThat(saved.getValue().getProduct()).isSameAs(product);
    }

    @Test
    void normalisesTheAmountToTwoDecimalPlaces() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(leratosSavingsProduct("1000.00")));
        when(noticeRepository.save(any(WithdrawalNotice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalResponse response =
                service.createWithdrawal(new WithdrawalRequest(1L, new BigDecimal("100.5")), LERATO);

        assertThat(response.amount()).isEqualTo(new BigDecimal("100.50"));
    }

    @Test
    void moneyOnHoldCountsAgainstTheNextNotice() {
        Product product = leratosSavingsProduct("10000.00");
        WithdrawalNotice.submit(product, new BigDecimal("5000.00"), NOW); // an open notice leaves R 5,000 available
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // 90% of the available R 5,000 is R 4,500.
        assertThatThrownBy(() -> service.createWithdrawal(new WithdrawalRequest(1L, new BigDecimal("4500.01")), LERATO))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> assertThat(ex.getViolation())
                        .isEqualTo(RuleViolation.EXCEEDS_WITHDRAWAL_LIMIT));
        verify(noticeRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundForUnknownProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createWithdrawal(new WithdrawalRequest(99L, new BigDecimal("10.00")), LERATO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verifyNoInteractions(noticeRepository);
    }

    @Test
    void ruleViolationHoldsNothingAndSavesNothing() {
        Product product = leratosSavingsProduct("1000.00");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.createWithdrawal(new WithdrawalRequest(1L, new BigDecimal("950.00")), LERATO))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(product.getHeldAmount()).isEqualByComparingTo("0.00");
        verify(noticeRepository, never()).save(any());
    }

    @Test
    void investorCannotWithdrawFromAnotherInvestorsProduct() {
        Product product = leratosSavingsProduct("1000.00");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        AuthenticatedUser someoneElse = TestUsers.investor(99L);
        assertThatThrownBy(
                        () -> service.createWithdrawal(new WithdrawalRequest(1L, new BigDecimal("10.00")), someoneElse))
                .isInstanceOf(AccessForbiddenException.class);

        assertThat(product.getHeldAmount()).isEqualByComparingTo("0.00");
        verify(noticeRepository, never()).save(any());
    }

    @Test
    void staffAccountsCannotSubmitWithdrawals() {
        assertThatThrownBy(() -> service.createWithdrawal(new WithdrawalRequest(1L, new BigDecimal("10.00")), STAFF))
                .isInstanceOf(AccessForbiddenException.class);

        verifyNoInteractions(productRepository, noticeRepository);
    }

    // ---- The workflow ----

    @Test
    void staffApproveAndThenPayANotice() {
        Product product = leratosSavingsProduct("10000.00");
        existingNotice(product, "2500.00");

        WithdrawalResponse approved = service.approve(5L, STAFF);

        assertThat(approved.status()).isEqualTo(NoticeStatus.APPROVED);
        assertThat(approved.reviewedBy()).isEqualTo(STAFF.getUsername());
        assertThat(approved.reviewedAt()).isEqualTo(NOW);
        assertThat(product.getBalance()).isEqualByComparingTo("10000.00");

        WithdrawalResponse paid = service.pay(5L, STAFF);

        assertThat(paid.status()).isEqualTo(NoticeStatus.PAID);
        assertThat(paid.balanceBefore()).isEqualByComparingTo("10000.00");
        assertThat(paid.balanceAfter()).isEqualByComparingTo("7500.00");
        assertThat(paid.paidAt()).isEqualTo(NOW);
        assertThat(product.getBalance()).isEqualByComparingTo("7500.00");
        assertThat(product.getHeldAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectingStoresTheTrimmedReasonAndReleasesTheHold() {
        Product product = leratosSavingsProduct("10000.00");
        existingNotice(product, "2500.00");

        WithdrawalResponse rejected = service.reject(5L, "  Bank details could not be verified.  ", STAFF);

        assertThat(rejected.status()).isEqualTo(NoticeStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Bank details could not be verified.");
        assertThat(product.getAvailableBalance()).isEqualByComparingTo("10000.00");
    }

    @Test
    void investorsCanCancelTheirOwnPendingNotice() {
        Product product = leratosSavingsProduct("10000.00");
        existingNotice(product, "2500.00");

        WithdrawalResponse cancelled = service.cancel(5L, LERATO);

        assertThat(cancelled.status()).isEqualTo(NoticeStatus.CANCELLED);
        assertThat(cancelled.cancelledAt()).isEqualTo(NOW);
        assertThat(product.getHeldAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void payingAPendingNoticeIsAConflict() {
        Product product = leratosSavingsProduct("10000.00");
        existingNotice(product, "2500.00");

        assertThatThrownBy(() -> service.pay(5L, STAFF))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage(
                        "Withdrawal notice #5 is pending, so it cannot be paid. Only approved notices can be paid.");
        assertThat(product.getBalance()).isEqualByComparingTo("10000.00");
    }

    @Test
    void investorsCannotApproveRejectOrPay() {
        assertThatThrownBy(() -> service.approve(5L, LERATO)).isInstanceOf(AccessForbiddenException.class);
        assertThatThrownBy(() -> service.reject(5L, "No", LERATO)).isInstanceOf(AccessForbiddenException.class);
        assertThatThrownBy(() -> service.pay(5L, LERATO)).isInstanceOf(AccessForbiddenException.class);

        verifyNoInteractions(noticeRepository);
    }

    @Test
    void staffCannotCancelANoticeForTheInvestor() {
        assertThatThrownBy(() -> service.cancel(5L, STAFF)).isInstanceOf(AccessForbiddenException.class);

        verifyNoInteractions(noticeRepository);
    }

    @Test
    void investorsCannotCancelSomeoneElsesNotice() {
        Product product = leratosSavingsProduct("10000.00");
        existingNotice(product, "2500.00");

        assertThatThrownBy(() -> service.cancel(5L, TestUsers.investor(99L)))
                .isInstanceOf(AccessForbiddenException.class);
        assertThat(product.getHeldAmount()).isEqualByComparingTo("2500.00");
    }

    @Test
    void unknownNoticeIsNotFound() {
        when(noticeRepository.findWithDetailsById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(404L, STAFF))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }

    // ---- helpers ----

    /** A pending notice with id 5, as the repository would return it. */
    private WithdrawalNotice existingNotice(Product product, String amount) {
        WithdrawalNotice notice = WithdrawalNotice.submit(product, new BigDecimal(amount), NOW.minusDays(1));
        ReflectionTestUtils.setField(notice, "id", 5L); // normally assigned by the database
        when(noticeRepository.findWithDetailsById(5L)).thenReturn(Optional.of(notice));
        return notice;
    }

    private static Product leratosSavingsProduct(String balance) {
        Investor investor = new Investor("Lerato", "Dlamini", "lerato@example.com", null, LocalDate.of(1986, 2, 1));
        ReflectionTestUtils.setField(investor, "id", LERATO_ID); // normally assigned by the database
        return new Product(investor, "Unit Trust Portfolio", ProductType.SAVINGS, new BigDecimal(balance));
    }
}
