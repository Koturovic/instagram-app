package com.instagram.user_service.repository;

import com.instagram.user_service.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerUserIdAndFollowingUserId(Long followerUserId, Long followingUserId);

    boolean existsByFollowerUserIdAndFollowingUserId(Long followerUserId, Long followingUserId);

    List<Follow> findByFollowerUserId(Long followerUserId);

    List<Follow> findByFollowingUserId(Long followingUserId);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.followerUserId = :followerId AND f.followingUserId = :followingId")
    void deleteByFollowerUserIdAndFollowingUserId(@Param("followerId") Long followerId, @Param("followingId") Long followingUserId);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.followerUserId = :userId OR f.followingUserId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    long countByFollowingUserId(Long followingUserId);

    long countByFollowerUserId(Long followerUserId);
}
