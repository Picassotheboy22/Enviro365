package com.enviro.assessment.junior.smsibi.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enviro.assessment.junior.smsibi.exception.InvalidStatusTransitionException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The notice workflow and the money it holds, tested directly on the entities: no Spring and no database, so these run
 * in milliseconds.
 */
class WithdrawalNoticeTest {

    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 9, 1, 9, 0);
    private static final String STAFF = "admin@enviro365.example";

    private final Product product = new Product(
            new Investor("Lerato", "Dlamini", "lerato@example.com", null, LocalDate.of(1986, 2, 1)),
            "Unit Trust Portfolio",
            ProductType.SAVINGS,
            new BigDecimal("10000.00"));

    @Test
    void onlyTheWorkflowMovesAreAllowed() {
        // Checks all 25 combinations, so a move added by mistake would fail this test.
        List<String> allowed = new ArrayList<>();
        for (NoticeStatus from : NoticeStatus.values()) {
            for (NoticeStatus to : NoticeStatus.values()) {
                if (from.canMoveTo(to)) {
                    allowed.add(from + " -> " + to);
                }
            }
        }

        assertThat(allowed)
                .containsExactlyInAnyOrder(
                        "PENDING -> APPROVED", "PENDING -> REJECTED", "PENDING -> CANCELLED", "APPROVED -> PAID");
    }

    @Test
    void onlyPendingAndApprovedNoticesAreOpen() {
        assertThat(Arrays.stream(NoticeStatus.values()).filter(NoticeStatus::isOpen))
                .containsExactly(NoticeStatus.PENDING, NoticeStatus.APPROVED);
    }

    @Test
    void submittingPutsTheAmountOnHoldWithoutChangingTheBalance() {
        WithdrawalNotice notice = submit("2500.00");

        assertThat(notice.getStatus()).isEqualTo(NoticeStatus.PENDING);
        assertThat(notice.getCreatedAt()).isEqualTo(SUBMITTED);
        assertThat(notice.getBalanceBefore()).isNull();
        assertThat(product.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(product.getHeldAmount()).isEqualByComparingTo("2500.00");
        assertThat(product.getAvailableBalance()).isEqualByComparingTo("7500.00");
    }

    @Test
    void approvingKeepsTheHoldAndRecordsTheReviewer() {
        WithdrawalNotice notice = submit("2500.00");

        notice.approve(STAFF, SUBMITTED.plusHours(2));

        assertThat(notice.getStatus()).isEqualTo(NoticeStatus.APPROVED);
        assertThat(notice.getReviewedBy()).isEqualTo(STAFF);
        assertThat(notice.getReviewedAt()).isEqualTo(SUBMITTED.plusHours(2));
        assertThat(product.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(product.getHeldAmount()).isEqualByComparingTo("2500.00");
    }

    @Test
    void payingDeductsTheBalanceReleasesTheHoldAndKeepsSnapshots() {
        WithdrawalNotice notice = submit("2500.00");
        notice.approve(STAFF, SUBMITTED.plusHours(2));

        notice.markPaid(STAFF, SUBMITTED.plusDays(1));

        assertThat(notice.getStatus()).isEqualTo(NoticeStatus.PAID);
        assertThat(notice.getBalanceBefore()).isEqualByComparingTo("10000.00");
        assertThat(notice.getBalanceAfter()).isEqualByComparingTo("7500.00");
        assertThat(notice.getPaidBy()).isEqualTo(STAFF);
        assertThat(notice.getPaidAt()).isEqualTo(SUBMITTED.plusDays(1));
        assertThat(product.getBalance()).isEqualByComparingTo("7500.00");
        assertThat(product.getHeldAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectingReleasesTheHoldAndKeepsTheReason() {
        WithdrawalNotice notice = submit("2500.00");

        notice.reject(STAFF, SUBMITTED.plusHours(2), "Bank details could not be verified.");

        assertThat(notice.getStatus()).isEqualTo(NoticeStatus.REJECTED);
        assertThat(notice.getRejectionReason()).isEqualTo("Bank details could not be verified.");
        assertThat(notice.getReviewedBy()).isEqualTo(STAFF);
        assertThat(product.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(product.getHeldAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void cancellingReleasesTheHold() {
        WithdrawalNotice notice = submit("2500.00");

        notice.cancel(SUBMITTED.plusHours(1));

        assertThat(notice.getStatus()).isEqualTo(NoticeStatus.CANCELLED);
        assertThat(notice.getCancelledAt()).isEqualTo(SUBMITTED.plusHours(1));
        assertThat(product.getAvailableBalance()).isEqualByComparingTo("10000.00");
    }

    @Test
    void aNoticeCannotBePaidBeforeItIsApproved() {
        WithdrawalNotice notice = submit("2500.00");

        assertThatThrownBy(() -> notice.markPaid(STAFF, SUBMITTED))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage(
                        "This withdrawal notice is pending, so it cannot be paid. Only approved notices can be paid.");
        assertThat(product.getBalance()).isEqualByComparingTo("10000.00");
    }

    @Test
    void anApprovedNoticeCanNoLongerBeCancelled() {
        WithdrawalNotice notice = submit("2500.00");
        notice.approve(STAFF, SUBMITTED);

        assertThatThrownBy(() -> notice.cancel(SUBMITTED)).isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(notice.getStatus()).isEqualTo(NoticeStatus.APPROVED);
        assertThat(product.getHeldAmount()).isEqualByComparingTo("2500.00");
    }

    @Test
    void aPaidNoticeCannotBePaidTwice() {
        WithdrawalNotice notice = submit("2500.00");
        notice.approve(STAFF, SUBMITTED);
        notice.markPaid(STAFF, SUBMITTED);

        assertThatThrownBy(() -> notice.markPaid(STAFF, SUBMITTED))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(product.getBalance()).isEqualByComparingTo("7500.00");
    }

    @Test
    void holdsCanNeverExceedTheAvailableBalance() {
        submit("6000.00");

        // WithdrawalPolicy would stop this first; the entity guards the invariant as well.
        assertThatThrownBy(() -> submit("4000.01")).isInstanceOf(IllegalStateException.class);
        assertThat(product.getHeldAmount()).isEqualByComparingTo("6000.00");
    }

    private WithdrawalNotice submit(String amount) {
        return WithdrawalNotice.submit(product, new BigDecimal(amount), SUBMITTED);
    }
}
