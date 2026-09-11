package com.enviro.assessment.junior.smsibi.repository;

import com.enviro.assessment.junior.smsibi.entity.ProductType;
import java.math.BigDecimal;

/** One row of {@link ProductRepository#totalsPerProductType()} (an interface projection). */
public interface ProductTypeTotals {

    ProductType getType();

    Long getProductCount();

    BigDecimal getTotalBalance();
}
