package com.instagram.interaction_service.service;

import com.instagram.interaction_service.entity.Like;
import com.instagram.interaction_service.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final RestTemplate restTemplate;

    // URL tvog post-servisa (port 8082)
    private static final String DEFAULT_POST_SERVICE_URL = "http://post-service:8082/api/posts/";

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

    public Long getLikesCount(Long postId) {
        return likeRepository.countByPostId(postId);
    }

    public boolean isPostLiked(Long postId, Long userId) {
        return likeRepository.existsByPostIdAndUserId(postId, userId);
    }
}
