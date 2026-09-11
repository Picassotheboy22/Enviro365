package com.enviro.assessment.junior.smsibi.repository;

import com.enviro.assessment.junior.smsibi.entity.Investor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestorRepository extends JpaRepository<Investor, Long> {

    // Spring Data derives the query from the method name: ORDER BY last_name, first_name.
    List<Investor> findAllByOrderByLastNameAscFirstNameAsc();

    Optional<Investor> findByEmail(String email);

    /** How many investors were born on or before this date (used to count who is old enough for retirement). */
    long countByDateOfBirthLessThanEqual(LocalDate date);
}
