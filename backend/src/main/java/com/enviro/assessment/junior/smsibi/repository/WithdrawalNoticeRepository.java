package com.enviro.assessment.junior.smsibi.repository;

import com.enviro.assessment.junior.smsibi.entity.WithdrawalNotice;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * JpaSpecificationExecutor lets the history/export queries be built dynamically from whichever
 * filters the user supplied (see {@link WithdrawalSpecifications}).
 */
public interface WithdrawalNoticeRepository
        extends JpaRepository<WithdrawalNotice, Long>, JpaSpecificationExecutor<WithdrawalNotice> {

    // Every history row shows the product and investor, so fetch them in the same SQL query.
    // Without the entity graph, each row would trigger extra queries to load them lazily (the "N+1 problem").
    @Override
    @EntityGraph(attributePaths = {"product", "product.investor"})
    List<WithdrawalNotice> findAll(Specification<WithdrawalNotice> spec, Sort sort);

    @EntityGraph(attributePaths = {"product", "product.investor"})
    Optional<WithdrawalNotice> findWithDetailsById(Long id);

    /** One row per investor who has withdrawn: number of notices, total withdrawn and the latest notice's time. */
    @Query("""
            select w.product.investor.id as investorId, count(w) as withdrawalCount,
                   sum(w.amount) as totalWithdrawn, max(w.createdAt) as lastWithdrawalAt
            from WithdrawalNotice w
            group by w.product.investor.id
            """)
    List<InvestorWithdrawalTotals> totalsPerInvestor();

    /** Number of notices and total withdrawn across all clients (always exactly one row). */
    @Query("select count(w) as noticeCount, sum(w.amount) as totalAmount from WithdrawalNotice w")
    NoticeTotals overallTotals();

    /**
     * Just the two values the dashboard chart needs, for notices submitted on or after {@code since}. Selecting only
     * these columns, rather than whole entities with their product and investor, keeps the query light.
     */
    @Query("select w.createdAt as createdAt, w.amount as amount from WithdrawalNotice w where w.createdAt >= :since")
    List<NoticeAmount> amountsSince(@Param("since") LocalDateTime since);
}
