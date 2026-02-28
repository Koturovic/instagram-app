package com.instagram.user_service.repository;

import com.instagram.user_service.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("SELECT p FROM Profile p WHERE LOWER(p.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(p.displayName) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Profile> searchByUsernameOrDisplayName(@Param("q") String query);
}
