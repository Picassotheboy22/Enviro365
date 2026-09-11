package com.enviro.assessment.junior.smsibi.dto;

import com.enviro.assessment.junior.smsibi.entity.ProductType;
import java.math.BigDecimal;
import java.util.List;

/**
 * Headline statistics for the staff dashboard. Everything is calculated on the server from the database, so the browser
 * never has to download every notice just to count them.
 *
 * @param retirementEligibleClients investors older than 65, who may withdraw from retirement products
 * @param averageWithdrawal         total withdrawn divided by the number of notices (0 when there are none)
 * @param withdrawalsByMonth        the last six calendar months, oldest first, including months without notices
 */
public record DashboardResponse(
        long clientCount,
        long productCount,
        BigDecimal assetsUnderManagement,
        long retirementEligibleClients,
        long noticeCount,
        BigDecimal totalWithdrawn,
        BigDecimal averageWithdrawal,
        long noticesLast30Days,
        BigDecimal withdrawnLast30Days,
        List<ProductTypeTotal> assetsByProductType,
        List<MonthlyWithdrawals> withdrawalsByMonth) {

    public record ProductTypeTotal(ProductType type, long productCount, BigDecimal balance) {}

    /** @param month the calendar month as "yyyy-MM", e.g. "2026-09" */
    public record MonthlyWithdrawals(String month, long noticeCount, BigDecimal amount) {}
}
