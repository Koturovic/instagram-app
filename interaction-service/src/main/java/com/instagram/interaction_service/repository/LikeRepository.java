package com.instagram.interaction_service.repository;

import com.instagram.interaction_service.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    // Provera da li lajk već postoji (za "unlike" logiku ili sprečavanje dupliranja)
    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    // Dohvatanje svih lajkova za određeni post (za filtriranje blokiranih korisnika)
    List<Like> findByPostId(Long postId);

    // Brojanje lajkova za određeni post
    Long countByPostId(Long postId);
}
