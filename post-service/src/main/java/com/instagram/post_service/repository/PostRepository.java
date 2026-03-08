package com.instagram.post_service.repository;

import com.instagram.post_service.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    // Spring će sam generisati: SELECT * FROM posts WHERE user_id = ?
    List<Post> findByUserId(Long userId);
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Post> findAllByOrderByCreatedAtDesc();
}