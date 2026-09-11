package com.enviro.assessment.junior.smsibi.mapper;

import com.enviro.assessment.junior.smsibi.dto.InvestorDetails;
import com.enviro.assessment.junior.smsibi.dto.InvestorSummary;
import com.enviro.assessment.junior.smsibi.dto.ProductResponse;
import com.enviro.assessment.junior.smsibi.dto.WithdrawalResponse;
import com.enviro.assessment.junior.smsibi.entity.Investor;
import com.enviro.assessment.junior.smsibi.entity.Product;
import com.enviro.assessment.junior.smsibi.entity.WithdrawalNotice;
import com.enviro.assessment.junior.smsibi.repository.InvestorProductTotals;
import com.enviro.assessment.junior.smsibi.repository.InvestorWithdrawalTotals;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Converts JPA entities into API DTOs.
 *
 * <p>Why a DTO layer at all? Returning entities directly would (a) couple the public JSON contract to the
 * database schema, (b) risk lazy-loading errors or infinite recursion during JSON serialisation, and
 * (c) leak internal fields such as the optimistic-lock version. Mapping is hand-written rather than
 * generated (e.g. MapStruct) because there are only a handful of types and it keeps the code explicit.
 */
public final class DtoMapper {

    private DtoMapper() {}

    /**
     * @param productTotals    the investor's product totals, or {@code null} if they hold no products
     * @param withdrawalTotals the investor's withdrawal totals, or {@code null} if they have never withdrawn
     */
    public static InvestorSummary toSummary(
            Investor investor,
            LocalDate today,
            InvestorProductTotals productTotals,
            InvestorWithdrawalTotals withdrawalTotals) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        return new InvestorSummary(
                investor.getId(),
                investor.getFullName(),
                investor.getEmail(),
                investor.ageOn(today),
                productTotals == null ? 0 : productTotals.getProductCount().intValue(),
                productTotals == null ? zero : productTotals.getTotalBalance(),
                withdrawalTotals == null ? 0 : withdrawalTotals.getWithdrawalCount(),
                withdrawalTotals == null ? zero : withdrawalTotals.getTotalWithdrawn(),
                withdrawalTotals == null ? null : withdrawalTotals.getLastWithdrawalAt());
    }

    public static InvestorDetails toDetails(Investor investor, LocalDate today) {
        return new InvestorDetails(
                investor.getId(),
                investor.getFirstName(),
                investor.getLastName(),
                investor.getFullName(),
                investor.getEmail(),
                investor.getPhone(),
                investor.getDateOfBirth(),
                investor.ageOn(today));
    }

    /**
     * @param restrictionReason why the product cannot be withdrawn from, or {@code null} if it can
     */
    public static ProductResponse toProductResponse(
            Product product, BigDecimal maxWithdrawalAmount, String restrictionReason) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getType(),
                product.getBalance(),
                maxWithdrawalAmount,
                restrictionReason == null,
                restrictionReason);
    }

    public static WithdrawalResponse toWithdrawalResponse(WithdrawalNotice notice) {
        Product product = notice.getProduct();
        Investor investor = product.getInvestor();
        return new WithdrawalResponse(
                notice.getId(),
                investor.getId(),
                investor.getFullName(),
                product.getId(),
                product.getName(),
                product.getType(),
                notice.getAmount(),
                notice.getBalanceBefore(),
                notice.getBalanceAfter(),
                notice.getCreatedAt());
    }
}
