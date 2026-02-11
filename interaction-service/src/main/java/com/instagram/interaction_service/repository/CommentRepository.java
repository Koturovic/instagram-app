package com.instagram.interaction_service.repository;

import com.instagram.interaction_service.entity.Comment; // Ovo je ključno!
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId);
}