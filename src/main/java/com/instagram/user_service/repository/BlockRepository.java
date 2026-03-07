package com.instagram.user_service.repository;

import com.instagram.user_service.domain.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    Optional<Block> findByBlockerUserIdAndBlockedUserId(Long blockerUserId, Long blockedUserId);

    boolean existsByBlockerUserIdAndBlockedUserId(Long blockerUserId, Long blockedUserId);

    List<Block> findByBlockerUserId(Long blockerUserId);

    List<Block> findByBlockedUserId(Long blockedUserId);

    @Query("SELECT b.blockedUserId FROM Block b WHERE b.blockerUserId = :blockerId")
    List<Long> findBlockedUserIdsByBlockerUserId(@Param("blockerId") Long blockerUserId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Block b " +
           "WHERE (b.blockerUserId = :id1 AND b.blockedUserId = :id2) OR (b.blockerUserId = :id2 AND b.blockedUserId = :id1)")
    boolean existsBlockBetween(@Param("id1") Long userId1, @Param("id2") Long userId2);

    @Modifying
    @Query("DELETE FROM Block b WHERE b.blockerUserId = :blockerId AND b.blockedUserId = :blockedId")
    void deleteByBlockerUserIdAndBlockedUserId(@Param("blockerId") Long blockerUserId, @Param("blockedId") Long blockedUserId);
}
