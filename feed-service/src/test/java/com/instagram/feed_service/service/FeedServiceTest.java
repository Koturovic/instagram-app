package com.instagram.feed_service.service;

import com.instagram.feed_service.dto.FeedResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FeedService feedService;

    @Test
    void getUserFeed_aggregatesAndSorts() {
        Long userId = 1L;

        List<Map<String, Object>> followingResponse = List.of(mapWith("userId", 2L));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)
        )).thenReturn(ResponseEntity.ok(followingResponse));

        when(restTemplate.getForObject(anyString(), eq(Long.class))).thenReturn(5L);

        when(restTemplate.getForObject(anyString(), eq(List.class))).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            if (url.contains("/api/posts/user/2")) {
                return List.of(
                        mapWith("id", 22L, "userId", 2L, "description", "post-2", "createdAt", "2026-01-02T10:00:00")
                );
            }
            if (url.contains("/api/posts/user/1")) {
                return List.of(
                        mapWith("id", 11L, "userId", 1L, "description", "post-1", "createdAt", "2026-01-03T08:00:00")
                );
            }
            if (url.contains("/api/comments/")) {
                return List.of();
            }
            return List.of();
        });

        List<FeedResponseDTO> feed = feedService.getUserFeed(userId, "Bearer token");

        assertEquals(2, feed.size());
        assertEquals(11L, feed.get(0).getPostId());
        assertEquals(22L, feed.get(1).getPostId());
    }

    @Test
    void getUserFeed_returnsEmptyWhenNoFollowingAndNoOwnPosts() {
        Long userId = 1L;

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)
        )).thenReturn(ResponseEntity.ok(List.of()));

        when(restTemplate.getForObject(anyString(), eq(List.class))).thenReturn(List.of());

        List<FeedResponseDTO> feed = feedService.getUserFeed(userId, "Bearer token");

        assertEquals(0, feed.size());
    }

    @Test
    void getUserFeed_handlesPostServiceError() {
        Long userId = 1L;

        List<Map<String, Object>> followingResponse = List.of(mapWith("userId", 2L));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)
        )).thenReturn(ResponseEntity.ok(followingResponse));

        when(restTemplate.getForObject(anyString(), eq(List.class))).thenThrow(new RuntimeException("Post service down"));

        List<FeedResponseDTO> feed = feedService.getUserFeed(userId, "Bearer token");

        assertEquals(0, feed.size());
    }

    @Test
    void getUserFeed_handlesInteractionServiceError() {
        Long userId = 1L;

        List<Map<String, Object>> followingResponse = List.of(mapWith("userId", 2L));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)
        )).thenReturn(ResponseEntity.ok(followingResponse));

        when(restTemplate.getForObject(anyString(), eq(Long.class))).thenThrow(new RuntimeException("Interaction service down"));

        when(restTemplate.getForObject(anyString(), eq(List.class))).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            if (url.contains("/api/posts/user/2")) {
                return List.of(
                        mapWith("id", 22L, "userId", 2L, "description", "post-2", "createdAt", "2026-01-02T10:00:00")
                );
            }
            if (url.contains("/api/posts/user/1")) {
                return List.of(); // No posts for user 1
            }
            if (url.contains("/api/comments/")) {
                return List.of();
            }
            return List.of();
        });

        List<FeedResponseDTO> feed = feedService.getUserFeed(userId, "Bearer token");

        assertEquals(1, feed.size());
        assertEquals(0L, feed.get(0).getLikesCount());
    }

    @Test
    void getUserFeed_sortsByCreatedAtDesc() {
        Long userId = 1L;

        List<Map<String, Object>> followingResponse = List.of(mapWith("userId", 2L));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)
        )).thenReturn(ResponseEntity.ok(followingResponse));

        when(restTemplate.getForObject(anyString(), eq(Long.class))).thenReturn(0L);

        when(restTemplate.getForObject(anyString(), eq(List.class))).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            if (url.contains("/api/posts/user/2")) {
                return List.of(
                        mapWith("id", 22L, "userId", 2L, "description", "post-2", "createdAt", "2026-01-01T10:00:00"),
                        mapWith("id", 23L, "userId", 2L, "description", "post-3", "createdAt", "2026-01-03T10:00:00")
                );
            }
            if (url.contains("/api/posts/user/1")) {
                return List.of(
                        mapWith("id", 11L, "userId", 1L, "description", "post-1", "createdAt", "2026-01-02T08:00:00")
                );
            }
            if (url.contains("/api/comments/")) {
                return List.of();
            }
            return List.of();
        });

        List<FeedResponseDTO> feed = feedService.getUserFeed(userId, "Bearer token");

        assertEquals(3, feed.size());
        assertEquals(23L, feed.get(0).getPostId()); // newest
        assertEquals(11L, feed.get(1).getPostId());
        assertEquals(22L, feed.get(2).getPostId()); // oldest
    }

    private static Map<String, Object> mapWith(Object... values) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
