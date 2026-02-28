package com.instagram.user_service.repository;

import com.instagram.user_service.domain.Follow;
import com.instagram.user_service.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerAndFollowing(Profile follower, Profile following);

    boolean existsByFollowerAndFollowing(Profile follower, Profile following);

    List<Follow> findByFollower(Profile follower);

    List<Follow> findByFollowing(Profile following);

    @Query("SELECT f FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    Optional<Follow> findByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.follower = :follower AND f.following = :following")
    void deleteByFollowerAndFollowing(@Param("follower") Profile follower, @Param("following") Profile following);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.follower.id = :profileId OR f.following.id = :profileId")
    void deleteAllByProfileId(@Param("profileId") Long profileId);
}
