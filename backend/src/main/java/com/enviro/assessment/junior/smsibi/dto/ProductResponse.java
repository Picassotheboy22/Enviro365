package com.enviro.assessment.junior.smsibi.dto;

import com.enviro.assessment.junior.smsibi.entity.ProductType;
import java.math.BigDecimal;

/**
 * A product as shown on the dashboard.
 *
 * <p>Besides the raw balance, the server tells the UI up-front what is allowed: {@code maxWithdrawalAmount}
 * (90% of the available balance, or 0 when restricted) and {@code withdrawalAllowed}/{@code restrictionReason}.
 * The UI can then guide the user before they submit, while the server still enforces the same rules on submit.
 *
 * @param heldAmount       the total of the product's open (pending or approved) withdrawal notices
 * @param availableBalance the balance minus the held amount: what new withdrawal notices can draw on
 */
public record ProductResponse(
        Long id,
        String name,
        ProductType type,
        BigDecimal balance,
        BigDecimal heldAmount,
        BigDecimal availableBalance,
        BigDecimal maxWithdrawalAmount,
        boolean withdrawalAllowed,
        String restrictionReason) {}
