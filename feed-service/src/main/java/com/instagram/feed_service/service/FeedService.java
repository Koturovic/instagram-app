package com.instagram.feed_service.service;

import com.instagram.feed_service.dto.FeedResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final RestTemplate restTemplate;
    private static final String DEFAULT_POST_SERVICE_URL = "http://post-service:8082";
    private static final String DEFAULT_INTERACTION_SERVICE_URL = "http://interaction-service:8083";
    private static final String DEFAULT_USER_SERVICE_URL = "http://user-service:8081";

    public List<FeedResponseDTO> getUserFeed(Long userId, String authorizationHeader) {
        List<FeedResponseDTO> feed = new ArrayList<>();

        String postServiceUrl = System.getenv().getOrDefault("APP_POST_SERVICE_URL", DEFAULT_POST_SERVICE_URL);
        String interactionServiceUrl = System.getenv().getOrDefault("APP_INTERACTION_SERVICE_URL", DEFAULT_INTERACTION_SERVICE_URL);
        String userServiceUrl = System.getenv().getOrDefault("APP_USER_SERVICE_URL", DEFAULT_USER_SERVICE_URL);

        List<Long> followingIds = new ArrayList<>();
        try {
            String followingUrl = userServiceUrl + "/api/v1/users/" + userId + "/following";
            HttpHeaders headers = new HttpHeaders();
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                headers.set("Authorization", authorizationHeader);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List> followingResponse = restTemplate.exchange(
                    followingUrl,
                    HttpMethod.GET,
                    entity,
                    List.class
            );
            List<java.util.Map<String, Object>> followingUsers = followingResponse.getBody();
            if (followingUsers != null) {
                for (java.util.Map<String, Object> followedUser : followingUsers) {
                    Object followedUserId = followedUser.get("userId");
                    if (followedUserId != null) {
                        followingIds.add(Long.valueOf(followedUserId.toString()));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Greška pri dohvatanju following liste: " + e.getMessage());
        }

        if (!followingIds.contains(userId)) {
            followingIds.add(userId);
        }

        for (Long fId : followingIds) {
            try {
                // 1. Uzmi postove od korisnika 1 sa tvog post-service-a (8082)
                String postUrl = postServiceUrl + "/api/posts/user/" + fId;
                List<java.util.Map<String, Object>> posts = restTemplate.getForObject(postUrl, List.class);

                if (posts != null) {
                    for (java.util.Map<String, Object> post : posts) {
                        Long postId = Long.valueOf(post.get("id").toString());
                        Object mediaFilesRaw = post.get("mediaFiles");
                        @SuppressWarnings("unchecked")
                        List<Object> mediaFiles = mediaFilesRaw instanceof List
                                ? (List<Object>) mediaFilesRaw
                                : List.of();

                        // 2. Uzmi broj lajkova sa tvog interaction-service-a (8083)
                        String likesUrl = interactionServiceUrl + "/api/likes/" + postId + "/count";
                        Long likes = 0L;
                        try {
                            Long likesResponse = restTemplate.getForObject(likesUrl, Long.class);
                            likes = likesResponse != null ? likesResponse : 0L;
                        } catch (Exception e) {
                            System.out.println("Greška pri dohvatanju broja lajkova za post " + postId + ": " + e.getMessage());
                        }

                        // 3. Uzmi komentare sa tvog interaction-service-a (8083)
                        String commentsUrl = interactionServiceUrl + "/api/comments/" + postId;
                        List<Object> comments = List.of();
                        try {
                            List<Object> commentsResponse = restTemplate.getForObject(commentsUrl, List.class);
                            comments = commentsResponse != null ? commentsResponse : List.of();
                        } catch (Exception e) {
                            System.out.println("Greška pri dohvatanju komentara za post " + postId + ": " + e.getMessage());
                        }

                        // 4. Pakovanje u DTO
                        Object createdAtRaw = post.get("createdAt");
                        String createdAt = createdAtRaw != null ? createdAtRaw.toString() : "";
                        feed.add(FeedResponseDTO.builder()
                                .postId(postId)
                                .userId(fId)
                                .description((String) post.get("description"))
                                .likesCount(likes)
                                .recentComments(comments)
                                .mediaFiles(mediaFiles)
                                .createdAt(createdAt)
                                .build());
                    }
                }
            } catch (Exception e) {
                System.out.println("Greška pri sakupljanju feed-a: " + e.getMessage());
            }
        }
        // Globalno sortiranje po createdAt DESC (najnovije prvo)
        feed.sort(Comparator.comparing(
                (FeedResponseDTO dto) -> dto.getCreatedAt() != null ? dto.getCreatedAt() : "",
                Comparator.reverseOrder()
        ));

        return feed;
    }
}
