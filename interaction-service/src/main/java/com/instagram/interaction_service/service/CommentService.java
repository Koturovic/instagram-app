package com.instagram.interaction_service.service;

import com.instagram.interaction_service.dto.RelationshipStatusResponse;
import com.instagram.interaction_service.entity.Comment;
import com.instagram.interaction_service.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final RestTemplate restTemplate;

    private static final String DEFAULT_POST_SERVICE_URL = "http://post-service:8082/api/posts/";
    private static final String DEFAULT_USER_SERVICE_URL = "http://user-service:8081/api/v1/users";

    public Comment addComment(Long postId, Long userId, String content) {
        // Provera postojanja posta je soft-fail; ne blokiramo komentar ako post-service nije dostupan.
        try {
            String postServiceUrl = System.getenv().getOrDefault("APP_POST_SERVICE_URL", DEFAULT_POST_SERVICE_URL);
            restTemplate.getForEntity(postServiceUrl + postId, Object.class);
        } catch (Exception ignored) {
            // ignore
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .userId(userId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsByPost(Long postId, String authHeader) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        if (authHeader == null) {
            return comments;
        }
        String userServiceUrl = System.getenv().getOrDefault("APP_USER_SERVICE_URL", DEFAULT_USER_SERVICE_URL);
        return comments.stream()
                .filter(comment -> !isBlocked(comment.getUserId(), authHeader, userServiceUrl))
                .collect(Collectors.toList());
    }

    public Long getCommentsCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    public Comment updateComment(Long commentId, Long userId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to edit this comment");
        }
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this comment");
        }
        commentRepository.deleteById(commentId);
    }

    private boolean isBlocked(Long userId, String authHeader, String userServiceUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<RelationshipStatusResponse> response = restTemplate.exchange(
                    userServiceUrl + "/relationship/" + userId,
                    HttpMethod.GET,
                    entity,
                    RelationshipStatusResponse.class
            );
            RelationshipStatusResponse status = response.getBody();
            return status != null && Boolean.TRUE.equals(status.getBlocked());
        } catch (Exception e) {
            return false;
        }
    }
}
