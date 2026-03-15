package com.instagram.interaction_service.service;

import com.instagram.interaction_service.dto.RelationshipStatusResponse;
import com.instagram.interaction_service.entity.Like;
import com.instagram.interaction_service.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final RestTemplate restTemplate;

    // URL tvog post-servisa (port 8082)
    private static final String DEFAULT_POST_SERVICE_URL = "http://post-service:8082/api/posts/";
    private static final String DEFAULT_USER_SERVICE_URL = "http://user-service:8081/api/v1/users";

    public String toggleLike(Long postId, Long userId) {
        // Provera postojanja je soft-fail; ne blokiramo like ako post-service nije dostupan.
        try {
            String postServiceUrl = System.getenv().getOrDefault("APP_POST_SERVICE_URL", DEFAULT_POST_SERVICE_URL);
            restTemplate.getForEntity(postServiceUrl + postId, Object.class);
        } catch (Exception ignored) {
            // ignore
        }

        // 2. LOGIKA ZA LAJK (ostaje ista ako post postoji)
        Optional<Like> existingLike = likeRepository.findByPostIdAndUserId(postId, userId);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            return "Post unliked";
        } else {
            Like newLike = Like.builder()
                    .postId(postId)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            likeRepository.save(newLike);
            return "Post liked";
        }
    }

    public Long getLikesCount(Long postId, String authHeader) {
        if (authHeader == null) {
            return likeRepository.countByPostId(postId);
        }
        String userServiceUrl = System.getenv().getOrDefault("APP_USER_SERVICE_URL", DEFAULT_USER_SERVICE_URL);
        List<Like> likes = likeRepository.findByPostId(postId);
        return likes.stream()
                .filter(like -> !isBlocked(like.getUserId(), authHeader, userServiceUrl))
                .count();
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

    public boolean isPostLiked(Long postId, Long userId) {
        return likeRepository.existsByPostIdAndUserId(postId, userId);
    }
}
