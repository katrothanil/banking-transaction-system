package com.anil.banking.repository;

import com.anil.banking.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /** Loads the user and its customer in one query (the JWT filter runs this on every request). */
    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.customer WHERE u.username = :username")
    Optional<AppUser> findByUsername(@Param("username") String username);

    boolean existsByUsername(String username);
}
