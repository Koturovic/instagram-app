package com.instagram.feed_service.service;

import com.instagram.feed_service.dto.FeedResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final RestTemplate restTemplate;
    private static final String DEFAULT_POST_SERVICE_URL = "http://post-service:8082";
    private static final String DEFAULT_INTERACTION_SERVICE_URL = "http://interaction-service:8083";

    public List<FeedResponseDTO> getUserFeed(Long userId) {
        List<FeedResponseDTO> feed = new ArrayList<>();

        // SIMULACIJA: Pošto user-service (8081) možda još nije gotov,
        // pretpostavićemo da pratiš korisnika sa ID-jem 1 (tebe)
        List<Long> followingIds = List.of(userId);

        String postServiceUrl = System.getenv().getOrDefault("APP_POST_SERVICE_URL", DEFAULT_POST_SERVICE_URL);
        String interactionServiceUrl = System.getenv().getOrDefault("APP_INTERACTION_SERVICE_URL", DEFAULT_INTERACTION_SERVICE_URL);

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
                        // Napomena: Proveri da li u LikeControlleru imaš ovaj endpoint, ako nemaš, napravićemo ga
                        Long likes = restTemplate.getForObject(likesUrl, Long.class);

                        // 3. Uzmi komentare sa tvog interaction-service-a (8083)
                        String commentsUrl = interactionServiceUrl + "/api/comments/" + postId;
                        List<Object> comments = restTemplate.getForObject(commentsUrl, List.class);

                        // 4. Pakovanje u DTO
                        feed.add(FeedResponseDTO.builder()
                                .postId(postId)
                                .userId(fId)
                                .description((String) post.get("description"))
                                .likesCount(likes != null ? likes : 0L)
                                .recentComments(comments)
                                .mediaFiles(mediaFiles)
                                .build());
                    }
                }
            } catch (Exception e) {
                System.out.println("Greška pri sakupljanju feed-a: " + e.getMessage());
            }
        }
        return feed;
    }
}
