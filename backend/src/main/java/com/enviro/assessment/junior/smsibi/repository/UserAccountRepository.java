package com.enviro.assessment.junior.smsibi.repository;

import com.enviro.assessment.junior.smsibi.entity.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    // The signed-in principal needs the investor's id, so load the investor in the same query.
    @EntityGraph(attributePaths = "investor")
    Optional<UserAccount> findByUsername(String username);
}
