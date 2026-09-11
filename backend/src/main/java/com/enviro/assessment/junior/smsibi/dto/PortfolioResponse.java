package com.enviro.assessment.junior.smsibi.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * An investor's portfolio: personal details plus all their products.
 */
public record PortfolioResponse(InvestorDetails investor, List<ProductResponse> products, BigDecimal totalBalance) {}
