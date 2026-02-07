package com.instagram.interaction_service.repository;

import com.instagram.interaction_service.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    // Provera da li lajk već postoji (za "unlike" logiku ili sprečavanje dupliranja)
    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);

    // Brojanje lajkova za određeni post
    Long countByPostId(Long postId);
}