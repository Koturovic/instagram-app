package com.instagram.interaction_service.service;

import com.instagram.interaction_service.dto.RelationshipStatusResponse;
import com.instagram.interaction_service.entity.Like;
import com.instagram.interaction_service.repository.LikeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LikeService likeService;

    @Test
    void toggleLike_deletesWhenExisting() {
        Like existing = Like.builder().id(1L).postId(10L).userId(7L).build();
        when(likeRepository.findByPostIdAndUserId(10L, 7L)).thenReturn(Optional.of(existing));

        String result = likeService.toggleLike(10L, 7L);

        assertEquals("Post unliked", result);
        verify(likeRepository).delete(existing);
    }

    @Test
    void toggleLike_savesWhenMissing() {
        when(likeRepository.findByPostIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        String result = likeService.toggleLike(10L, 7L);

        assertEquals("Post liked", result);
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    void getLikesCount_withoutAuthUsesCount() {
        when(likeRepository.countByPostId(5L)).thenReturn(4L);

        Long count = likeService.getLikesCount(5L, null);

        assertEquals(4L, count);
    }

    @Test
    void getLikesCount_filtersBlockedUsers() {
        List<Like> likes = List.of(
                Like.builder().id(1L).postId(5L).userId(1L).build(),
                Like.builder().id(2L).postId(5L).userId(2L).build()
        );
        when(likeRepository.findByPostId(5L)).thenReturn(likes);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(RelationshipStatusResponse.class)
        )).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            boolean blocked = url.endsWith("/relationship/2");
            return ResponseEntity.ok(new RelationshipStatusResponse(null, null, blocked));
        });

        Long count = likeService.getLikesCount(5L, "Bearer token");

        assertEquals(1L, count);
    }

    @Test
    void isPostLiked_returnsTrueWhenExists() {
        when(likeRepository.existsByPostIdAndUserId(5L, 7L)).thenReturn(true);

        boolean result = likeService.isPostLiked(5L, 7L);

        assertEquals(true, result);
    }

    @Test
    void isPostLiked_returnsFalseWhenNotExists() {
        when(likeRepository.existsByPostIdAndUserId(5L, 7L)).thenReturn(false);

        boolean result = likeService.isPostLiked(5L, 7L);

        assertEquals(false, result);
    }

    @Test
    void toggleLike_ignoresPostServiceError() {
        when(restTemplate.getForEntity(anyString(), eq(Object.class))).thenThrow(new RuntimeException("Post not found"));
        when(likeRepository.findByPostIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        String result = likeService.toggleLike(10L, 7L);

        assertEquals("Post liked", result);
        verify(likeRepository).save(any(Like.class));
    }
}
