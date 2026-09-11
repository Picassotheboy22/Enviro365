package com.enviro.assessment.junior.smsibi.repository;

import java.math.BigDecimal;

/**
 * One row of {@link ProductRepository#totalsPerInvestor()}. Spring Data fills this "interface projection" straight from
 * the query's aliased columns, so no entity objects are loaded just to add numbers up.
 */
public interface InvestorProductTotals {

    Long getInvestorId();

    Long getProductCount();

    BigDecimal getTotalBalance();
}
