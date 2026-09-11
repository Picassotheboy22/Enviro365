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
import com.enviro.assessment.junior.smsibi.entity.Product;
import com.enviro.assessment.junior.smsibi.entity.ProductType;
import com.enviro.assessment.junior.smsibi.entity.WithdrawalNotice;
import com.enviro.assessment.junior.smsibi.exception.AccessForbiddenException;
import com.enviro.assessment.junior.smsibi.exception.BusinessRuleException;
import com.enviro.assessment.junior.smsibi.exception.ResourceNotFoundException;
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
 * Tests the withdrawal use case (orchestration: load, check ownership, validate, deduct, save). The repositories are
 * mocked, but the real WithdrawalPolicy is used, because the rules are what we care about.
 */
@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    // A fixed clock makes "now" predictable: 10 Sep 2026, 10:00.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-10T10:00:00Z"), ZoneOffset.UTC);

    private static final long LERATO_ID = 7L;
    private static final AuthenticatedUser LERATO = TestUsers.investor(LERATO_ID);

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WithdrawalNoticeRepository noticeRepository;

    private WithdrawalService service;

    @BeforeEach
    void setUp() {
        service = new WithdrawalService(productRepository, noticeRepository, new WithdrawalPolicy(), CLOCK);
    }

    @Test
    void createsNoticeWithBalanceSnapshotsAndDeductsTheBalance() {
        Product product = leratosSavingsProduct("10000.00");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(noticeRepository.save(any(WithdrawalNotice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalResponse response =
                service.createWithdrawal(new WithdrawalRequest(1L, new BigDecimal("2500.00")), LERATO);

        assertThat(response.amount()).isEqualByComparingTo("2500.00");
        assertThat(response.balanceBefore()).isEqualByComparingTo("10000.00");
        assertThat(response.balanceAfter()).isEqualByComparingTo("7500.00");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 9, 10, 10, 0));
        assertThat(response.investorId()).isEqualTo(LERATO_ID);
        assertThat(product.getBalance()).isEqualByComparingTo("7500.00");

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
    void throwsNotFoundForUnknownProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createWithdrawal(new WithdrawalRequest(99L, new BigDecimal("10.00")), LERATO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verifyNoInteractions(noticeRepository);
    }

    @Test
    void ruleViolationLeavesBalanceUnchangedAndSavesNothing() {
        Product product = leratosSavingsProduct("1000.00");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.createWithdrawal(new WithdrawalRequest(1L, new BigDecimal("950.00")), LERATO))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(product.getBalance()).isEqualByComparingTo("1000.00");
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

        assertThat(product.getBalance()).isEqualByComparingTo("1000.00");
        verify(noticeRepository, never()).save(any());
    }

    @Test
    void staffAccountsCannotSubmitWithdrawals() {
        assertThatThrownBy(() ->
                        service.createWithdrawal(new WithdrawalRequest(1L, new BigDecimal("10.00")), TestUsers.admin()))
                .isInstanceOf(AccessForbiddenException.class);

        verifyNoInteractions(productRepository, noticeRepository);
    }

    private static Product leratosSavingsProduct(String balance) {
        Investor investor = new Investor("Lerato", "Dlamini", "lerato@example.com", null, LocalDate.of(1986, 2, 1));
        ReflectionTestUtils.setField(investor, "id", LERATO_ID); // normally assigned by the database
        return new Product(investor, "Unit Trust Portfolio", ProductType.SAVINGS, new BigDecimal(balance));
    }
}
