package com.enviro.assessment.junior.smsibi.repository;

import com.enviro.assessment.junior.smsibi.entity.NoticeStatus;
import java.math.BigDecimal;

/** One row of {@link WithdrawalNoticeRepository#totalsPerStatus()} (an interface projection). */
public interface StatusTotals {

    NoticeStatus getStatus();

    Long getNoticeCount();

    BigDecimal getTotalAmount();
}
