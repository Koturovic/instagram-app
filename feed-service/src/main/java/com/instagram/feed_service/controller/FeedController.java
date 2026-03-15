package com.instagram.feed_service.controller;

import com.instagram.feed_service.dto.FeedResponseDTO;
import com.instagram.feed_service.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    // Endpoint za dohvatanje feed-a za određenog korisnika
    // Poziva se sa: GET http://localhost:8084/api/feed/{userId}
    @GetMapping("/{userId}")
    public List<FeedResponseDTO> getFeed(@PathVariable Long userId) {
        return feedService.getUserFeed(userId);
    }
}