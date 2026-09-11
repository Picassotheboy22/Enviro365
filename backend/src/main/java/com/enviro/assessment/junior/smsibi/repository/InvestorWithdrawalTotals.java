package com.enviro.assessment.junior.smsibi.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of {@link WithdrawalNoticeRepository#totalsPerInvestor()} (an interface projection). */
public interface InvestorWithdrawalTotals {

    Long getInvestorId();

    Long getWithdrawalCount();

    BigDecimal getTotalWithdrawn();

    LocalDateTime getLastWithdrawalAt();
}
