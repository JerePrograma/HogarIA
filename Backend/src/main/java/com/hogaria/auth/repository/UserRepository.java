package com.hogaria.auth.repository;

import com.hogaria.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for User entities.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username.
     *
     * @param username unique username
     * @return optional user
     */
    Optional<User> findByUsername(String username);
}
