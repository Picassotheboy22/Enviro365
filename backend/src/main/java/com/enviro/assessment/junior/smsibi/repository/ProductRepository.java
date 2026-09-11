package com.enviro.assessment.junior.smsibi.repository;

import com.enviro.assessment.junior.smsibi.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByInvestorIdOrderByIdAsc(Long investorId);

    /**
     * One row per investor with products: how many products they hold and the combined balance. The database does the
     * counting and adding in a single query (GROUP BY), instead of the application loading every product.
     */
    @Query("""
            select p.investor.id as investorId, count(p) as productCount, sum(p.balance) as totalBalance
            from Product p
            group by p.investor.id
            """)
    List<InvestorProductTotals> totalsPerInvestor();

    /** One row per product type (retirement, savings): number of products and their combined balance. */
    @Query("""
            select p.type as type, count(p) as productCount, sum(p.balance) as totalBalance
            from Product p
            group by p.type
            order by p.type
            """)
    List<ProductTypeTotals> totalsPerProductType();
}
