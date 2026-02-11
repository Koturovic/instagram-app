package com.instagram.interaction_service.service;

import com.instagram.interaction_service.entity.Comment;
import com.instagram.interaction_service.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final RestTemplate restTemplate;

    private final String POST_SERVICE_URL = "http://localhost:8082/api/posts/";

    public Comment addComment(Long postId, Long userId, String content) {
        // Provera postojanja posta preko Post-Servisa
        try {
            restTemplate.getForEntity(POST_SERVICE_URL + postId, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Greška: Post sa ID-jem " + postId + " ne postoji.");
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .userId(userId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsByPost(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}