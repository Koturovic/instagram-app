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
    private final String POST_SERVICE_URL = "http://localhost:8082/api/posts/";

    public String toggleLike(Long postId, Long userId) {
        // 1. PROVERA: Da li post postoji u post-service?
        try {
            // Šaljemo GET zahtev na npr. http://localhost:8082/api/posts/1
            // Ako post ne postoji, post-service će baciti 404, što RestTemplate hvata kao Exception
            restTemplate.getForEntity(POST_SERVICE_URL + postId, Object.class);
        } catch (HttpClientErrorException.NotFound e) {
            return "Greška: Post sa ID-jem " + postId + " ne postoji u bazi objava.";
        } catch (Exception e) {
            return "Greška: Ne mogu da kontaktiram Post-Service. Proverite da li je servis upaljen.";
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
}