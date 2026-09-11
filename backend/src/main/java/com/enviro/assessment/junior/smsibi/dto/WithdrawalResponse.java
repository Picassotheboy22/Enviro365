package com.enviro.assessment.junior.smsibi.dto;

import com.enviro.assessment.junior.smsibi.entity.ProductType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A withdrawal notice as returned by the API (creation response, history rows and CSV rows).
 * It is flattened, so the UI does not need extra calls to show the product or investor name.
 */
public record WithdrawalResponse(
        Long id,
        Long investorId,
        String investorName,
        Long productId,
        String productName,
        ProductType productType,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        LocalDateTime createdAt) {}
