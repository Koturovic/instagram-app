package com.instagram.user_service.controller;

import com.instagram.user_service.dto.CountResponse;
import com.instagram.user_service.dto.FollowRequestResponse;
import com.instagram.user_service.security.CurrentUser;
import com.instagram.user_service.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final FollowService followService;

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

    @PostMapping("/follow-request/{targetUserId}")
    public ResponseEntity<FollowRequestResponse> sendFollowRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long targetUserId) {
        FollowRequestResponse response = followService.sendFollowRequest(currentUser.getUserId(), targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/follow-request/{requestId}/accept")
    public ResponseEntity<Void> acceptFollowRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long requestId) {
        followService.acceptFollowRequest(currentUser.getUserId(), requestId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/following/{targetUserId}")
    public ResponseEntity<Void> unfollow(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long targetUserId) {
        followService.unfollow(currentUser.getUserId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/block/{targetUserId}")
    public ResponseEntity<Void> block(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long targetUserId) {
        followService.block(currentUser.getUserId(), targetUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/block/{targetUserId}")
    public ResponseEntity<Void> unblock(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long targetUserId) {
        followService.unblock(currentUser.getUserId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Placeholder: pretraga (vraća praznu listu). Kasnije: poziv auth-service search + filter po block listi.
     */
    @GetMapping("/search")
    public ResponseEntity<List<?>> search(@RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(Collections.emptyList());
    }
}
