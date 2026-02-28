package com.instagram.user_service.repository;

import com.instagram.user_service.domain.Block;
import com.instagram.user_service.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    Optional<Block> findByBlockerAndBlocked(Profile blocker, Profile blocked);

    boolean existsByBlockerAndBlocked(Profile blocker, Profile blocked);

    @Query("SELECT b FROM Block b WHERE b.blocker.id = :blockerId AND b.blocked.id = :blockedId")
    Optional<Block> findByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    @Query("SELECT b.blocked FROM Block b WHERE b.blocker.id = :blockerId")
    List<Profile> findBlockedProfilesByBlockerId(@Param("blockerId") Long blockerId);

    @Query("SELECT b.blocker FROM Block b WHERE b.blocked.id = :blockedId")
    List<Profile> findBlockerProfilesByBlockedId(@Param("blockedId") Long blockedId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Block b " +
           "WHERE (b.blocker.id = :id1 AND b.blocked.id = :id2) OR (b.blocker.id = :id2 AND b.blocked.id = :id1)")
    boolean existsBlockBetween(@Param("id1") Long profileId1, @Param("id2") Long profileId2);
}
