package com.enviro.assessment.junior.smsibi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the staff "Clients" overview: who the investor is, plus totals, so staff can see at a glance who holds
 * what and who has been withdrawing, without opening every portfolio.
 *
 * @param withdrawalCount  withdrawal notices submitted, whatever their status
 * @param openNoticeCount  notices that are still pending or approved, i.e. waiting for staff
 * @param totalWithdrawn   the total paid out
 * @param lastWithdrawalAt when the latest withdrawal notice was submitted, or {@code null} if there are none
 */
public record InvestorSummary(
        Long id,
        String fullName,
        String email,
        int age,
        int productCount,
        BigDecimal totalBalance,
        long withdrawalCount,
        long openNoticeCount,
        BigDecimal totalWithdrawn,
        LocalDateTime lastWithdrawalAt) {}
