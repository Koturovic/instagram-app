package com.instagram.user_service.controller;

import com.instagram.user_service.dto.CountResponse;
import com.instagram.user_service.dto.FollowRequestResponse;
import com.instagram.user_service.security.CurrentUser;
import com.instagram.user_service.service.BlockService;
import com.instagram.user_service.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final FollowService followService;
    private final BlockService blockService;

    private static String getBearerToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof String token) {
            return token;
        }
        return null;
    }

    @PostMapping("/follow-request/{targetUserId}")
    public ResponseEntity<FollowRequestResponse> sendFollowRequest(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        String token = getBearerToken();
        Long requestId = followService.sendFollowRequest(targetUserId, currentUser, token);
        return ResponseEntity
                .created(URI.create("/api/v1/users/follow-request/" + targetUserId + "/" + requestId))
                .body(new FollowRequestResponse(requestId));
    }

    @PostMapping("/follow-request/{requestId}/accept")
    public ResponseEntity<Void> acceptFollowRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        followService.acceptFollowRequest(requestId, currentUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/following/{targetUserId}")
    public ResponseEntity<Void> unfollow(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        followService.unfollow(targetUserId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/block/{targetUserId}")
    public ResponseEntity<Void> block(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        blockService.block(targetUserId, currentUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/block/{targetUserId}")
    public ResponseEntity<Void> unblock(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        blockService.unblock(targetUserId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/followers/count")
    public ResponseEntity<CountResponse> getFollowersCount(@PathVariable Long userId) {
        long count = followService.getFollowersCount(userId);
        return ResponseEntity.ok(new CountResponse(count));
    }

    @GetMapping("/{userId}/following/count")
    public ResponseEntity<CountResponse> getFollowingCount(@PathVariable Long userId) {
        long count = followService.getFollowingCount(userId);
        return ResponseEntity.ok(new CountResponse(count));
    }

    /**
     * Pretraga – placeholder dok auth-service nema search.
     * Vraća praznu listu; kasnije može pozivati auth GET /api/v1/auth/search?q=...
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(java.util.List.of());
    }
}
