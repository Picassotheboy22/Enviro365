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

    /**
     * One row per investor who has submitted notices: how many (any status), how many are still open, the total paid
     * out and when the latest notice was submitted.
     *
     * <p>COUNT and SUM skip NULLs, and "case when ... then x end" is NULL when the condition is false, so each of those
     * columns only counts or adds up the notices that match. Enum values are referenced by their full class name.
     */
    @Query("""
            select w.product.investor.id as investorId,
                   count(w) as withdrawalCount,
                   count(case when w.status in (com.enviro.assessment.junior.smsibi.entity.NoticeStatus.PENDING,
                                                com.enviro.assessment.junior.smsibi.entity.NoticeStatus.APPROVED)
                              then 1 end) as openNoticeCount,
                   sum(case when w.status = com.enviro.assessment.junior.smsibi.entity.NoticeStatus.PAID
                            then w.amount end) as totalWithdrawn,
                   max(w.createdAt) as lastWithdrawalAt
            from WithdrawalNotice w
            group by w.product.investor.id
            """)
    List<InvestorWithdrawalTotals> totalsPerInvestor();

    /** The number of notices and their combined amount for each status (one row per status that occurs). */
    @Query("""
            select w.status as status, count(w) as noticeCount, sum(w.amount) as totalAmount
            from WithdrawalNotice w
            group by w.status
            """)
    List<StatusTotals> totalsPerStatus();

    /** Notices submitted on or after {@code since}, whatever their status. */
    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    /**
     * When and how much was paid out, for notices paid on or after {@code since} (paidAt is only set on paid notices).
     * Selecting just these two columns, rather than whole entities with their product and investor, keeps it light.
     */
    @Query("select w.paidAt as paidAt, w.amount as amount from WithdrawalNotice w where w.paidAt >= :since")
    List<PaidAmount> paidSince(@Param("since") LocalDateTime since);
}
