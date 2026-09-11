package com.enviro.assessment.junior.smsibi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enviro.assessment.junior.smsibi.dto.WithdrawalFilter;
import com.enviro.assessment.junior.smsibi.entity.NoticeStatus;
import com.enviro.assessment.junior.smsibi.exception.AccessForbiddenException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccessGuardTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);
    private static final List<NoticeStatus> OPEN = List.of(NoticeStatus.PENDING, NoticeStatus.APPROVED);

    @Test
    void investorsCanAccessOnlyTheirOwnPortfolio() {
        assertThatCode(() -> AccessGuard.requireInvestorAccess(TestUsers.investor(7L), 7L))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> AccessGuard.requireInvestorAccess(TestUsers.investor(7L), 8L))
                .isInstanceOf(AccessForbiddenException.class)
                .hasMessage("You can only access your own portfolio.");
    }

    @Test
    void staffCanAccessEveryPortfolio() {
        assertThatCode(() -> AccessGuard.requireInvestorAccess(TestUsers.admin(), 8L))
                .doesNotThrowAnyException();
    }

    @Test
    void investorHistoryIsAlwaysScopedToTheirOwnInvestorId() {
        WithdrawalFilter scoped =
                AccessGuard.restrictToUser(new WithdrawalFilter(null, 3L, FROM, TO, OPEN), TestUsers.investor(7L));

        // The other filters are kept as they were.
        assertThat(scoped).isEqualTo(new WithdrawalFilter(7L, 3L, FROM, TO, OPEN));
    }

    @Test
    void investorCannotAskForAnotherInvestorsHistory() {
        assertThatThrownBy(() -> AccessGuard.restrictToUser(
                        new WithdrawalFilter(8L, null, null, null, null), TestUsers.investor(7L)))
                .isInstanceOf(AccessForbiddenException.class);
    }

    @Test
    void staffFiltersAreLeftUnchanged() {
        WithdrawalFilter filter = new WithdrawalFilter(8L, null, FROM, null, OPEN);

        assertThat(AccessGuard.restrictToUser(filter, TestUsers.admin())).isSameAs(filter);
    }
}
