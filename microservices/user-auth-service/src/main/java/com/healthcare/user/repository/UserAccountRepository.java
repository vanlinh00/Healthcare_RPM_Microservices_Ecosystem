package com.healthcare.user.repository;

import com.healthcare.user.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM UserAccount u WHERE LOWER(u.email) = LOWER(:input) OR LOWER(u.email) LIKE LOWER(CONCAT(:input, '@%'))")
    Optional<UserAccount> findByEmailOrUsernamePrefix(@Param("input") String input);
}
