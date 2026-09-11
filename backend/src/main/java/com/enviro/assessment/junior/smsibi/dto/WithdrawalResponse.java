package com.enviro.assessment.junior.smsibi.dto;

import com.enviro.assessment.junior.smsibi.entity.NoticeStatus;
import com.enviro.assessment.junior.smsibi.entity.ProductType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A withdrawal notice as returned by the API (creation response, history rows and CSV rows).
 * It is flattened, so the UI does not need extra calls to show the product or investor name.
 *
 * @param balanceBefore   the product balance just before the notice was paid, or {@code null} until it is paid
 * @param balanceAfter    the product balance just after the notice was paid, or {@code null} until it is paid
 * @param createdAt       when the investor submitted the notice
 * @param reviewedBy      the staff member who approved or rejected the notice, or {@code null} while it is pending
 * @param rejectionReason why staff rejected the notice (shown to the investor), or {@code null}
 */
public record WithdrawalResponse(
        Long id,
        Long investorId,
        String investorName,
        Long productId,
        String productName,
        ProductType productType,
        BigDecimal amount,
        NoticeStatus status,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        LocalDateTime createdAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String rejectionReason,
        String paidBy,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt) {}
