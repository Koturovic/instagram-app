package com.instagram.auth.repository;

import com.instagram.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
