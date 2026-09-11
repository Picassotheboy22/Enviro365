package com.enviro.assessment.junior.smsibi.dto;

import com.enviro.assessment.junior.smsibi.entity.ProductType;
import java.math.BigDecimal;
import java.util.List;

/**
 * Headline statistics for the staff dashboard. Everything is calculated on the server from the database, so the browser
 * never has to download every notice just to count them.
 *
 * @param retirementEligibleClients investors older than 65, who may withdraw from retirement products
 * @param noticeCount               notices submitted, whatever their status
 * @param awaitingApproval          pending notices, which staff need to approve or reject
 * @param awaitingPayment           approved notices, which staff need to pay
 * @param amountOnHold              the total of all open (pending and approved) notices
 * @param paidCount                 notices that have been paid
 * @param totalWithdrawn            the total paid out
 * @param averageWithdrawal         the total paid out divided by the number of paid notices (0 when there are none)
 * @param noticesLast30Days         notices submitted in the last 30 days
 * @param withdrawnLast30Days       the amount paid out in the last 30 days
 * @param withdrawalsByMonth        payments in each of the last six calendar months, oldest first, including months
 *                                  without payments
 */
public record DashboardResponse(
        long clientCount,
        long productCount,
        BigDecimal assetsUnderManagement,
        long retirementEligibleClients,
        long noticeCount,
        long awaitingApproval,
        long awaitingPayment,
        BigDecimal amountOnHold,
        long paidCount,
        BigDecimal totalWithdrawn,
        BigDecimal averageWithdrawal,
        long noticesLast30Days,
        BigDecimal withdrawnLast30Days,
        List<ProductTypeTotal> assetsByProductType,
        List<MonthlyWithdrawals> withdrawalsByMonth) {

    public record ProductTypeTotal(ProductType type, long productCount, BigDecimal balance) {}

    /**
     * @param month       the calendar month as "yyyy-MM", e.g. "2026-09"
     * @param noticeCount notices paid in that month
     * @param amount      the amount paid out in that month
     */
    public record MonthlyWithdrawals(String month, long noticeCount, BigDecimal amount) {}
}
