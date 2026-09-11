package com.enviro.assessment.junior.smsibi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the staff "Clients" overview: who the investor is, plus totals, so staff can see at a glance who holds
 * what and who has been withdrawing, without opening every portfolio.
 *
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
        BigDecimal totalWithdrawn,
        LocalDateTime lastWithdrawalAt) {}
