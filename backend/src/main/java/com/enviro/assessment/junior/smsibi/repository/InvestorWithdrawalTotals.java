package com.enviro.assessment.junior.smsibi.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of {@link WithdrawalNoticeRepository#totalsPerInvestor()} (an interface projection). */
public interface InvestorWithdrawalTotals {

    Long getInvestorId();

    /** Notices submitted, whatever their status. */
    Long getWithdrawalCount();

    /** Notices that are still pending or approved. */
    Long getOpenNoticeCount();

    /** Total paid out, or {@code null} when none of the investor's notices has been paid (SUM of no rows is NULL). */
    BigDecimal getTotalWithdrawn();

    /** When the latest notice was submitted. */
    LocalDateTime getLastWithdrawalAt();
}
