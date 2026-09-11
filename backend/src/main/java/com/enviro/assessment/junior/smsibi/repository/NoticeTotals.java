package com.enviro.assessment.junior.smsibi.repository;

import java.math.BigDecimal;

/** The single row of {@link WithdrawalNoticeRepository#overallTotals()} (an interface projection). */
public interface NoticeTotals {

    Long getNoticeCount();

    /** {@code null} when there are no notices at all: in SQL, the SUM of zero rows is NULL, not 0. */
    BigDecimal getTotalAmount();
}
