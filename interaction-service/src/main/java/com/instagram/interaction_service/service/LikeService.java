package com.instagram.interaction_service.service;

import com.instagram.interaction_service.entity.Like;
import com.instagram.interaction_service.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;

    public String toggleLike(Long postId, Long userId) {
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