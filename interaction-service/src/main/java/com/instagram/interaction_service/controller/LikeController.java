package com.instagram.interaction_service.controller;

import com.instagram.interaction_service.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    // Endpoint za lajkovanje ili uklanjanje lajka (Toggle)
    // Poziva se sa: POST http://localhost:8083/api/likes/{postId}?userId={userId}
    @PostMapping("/{postId}")
    public String toggleLike(
            @PathVariable Long postId,
            @RequestParam Long userId) {
        return likeService.toggleLike(postId, userId);
    }

    // Endpoint za broj lajkova
    // Poziva se sa: GET http://localhost:8083/api/likes/{postId}/count
    @GetMapping("/{postId}/count")
    public Long getLikesCount(@PathVariable Long postId) {
        return likeService.getLikesCount(postId);
    }
}