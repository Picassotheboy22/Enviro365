package com.enviro.assessment.junior.smsibi.repository;

import com.enviro.assessment.junior.smsibi.dto.WithdrawalFilter;
import com.enviro.assessment.junior.smsibi.entity.WithdrawalNotice;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the WHERE clause for withdrawal history queries.
 *
 * <p>Every filter is optional: a {@code null} value means "do not filter on this". Building predicates
 * dynamically avoids writing one repository method per combination of filters.
 */
public final class WithdrawalSpecifications {

    private WithdrawalSpecifications() {}

    public static Specification<WithdrawalNotice> matching(WithdrawalFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.productId() != null) {
                predicates.add(cb.equal(root.get("product").get("id"), filter.productId()));
            }
            if (filter.investorId() != null) {
                predicates.add(cb.equal(root.get("product").get("investor").get("id"), filter.investorId()));
            }
            // Date filters are inclusive of whole days: "to = 2026-09-10" must include 23:59 on that day,
            // so we compare against midnight at the START of the following day.
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("createdAt"), filter.from().atStartOfDay()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThan(
                        root.<LocalDateTime>get("createdAt"),
                        filter.to().plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
