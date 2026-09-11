package com.enviro.assessment.junior.smsibi.dto;

import com.enviro.assessment.junior.smsibi.entity.ProductType;
import java.math.BigDecimal;

/**
 * A product as shown on the dashboard.
 *
 * <p>Besides the raw balance, the server tells the UI up-front what is allowed: {@code maxWithdrawalAmount}
 * (90% of the balance, or 0 when restricted) and {@code withdrawalAllowed}/{@code restrictionReason}.
 * The UI can then guide the user before they submit, while the server still enforces the same rules on submit.
 */
public record ProductResponse(
        Long id,
        String name,
        ProductType type,
        BigDecimal balance,
        BigDecimal maxWithdrawalAmount,
        boolean withdrawalAllowed,
        String restrictionReason) {}
