package com.instagram.user_service.controller;

import com.instagram.user_service.dto.CountResponse;
import com.instagram.user_service.dto.FollowUserDto;
import com.instagram.user_service.dto.FollowRequestResponse;
import com.instagram.user_service.dto.RelationshipStatusDto;
import com.instagram.user_service.dto.UserSearchResultDto;
import com.instagram.user_service.security.CurrentUser;
import com.instagram.user_service.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<FollowUserDto>> getFollowers(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowers(userId));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<List<FollowUserDto>> getFollowing(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowing(userId));
    }

    @GetMapping("/relationship/{targetUserId}")
    public ResponseEntity<RelationshipStatusDto> getRelationshipStatus(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long targetUserId) {
        Long currentUserId = currentUser != null ? currentUser.getUserId() : null;
        return ResponseEntity.ok(followService.getRelationshipStatus(currentUserId, targetUserId));
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

    @PostMapping("/follow-request/{requestId}/reject")
    public ResponseEntity<Void> rejectFollowRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long requestId) {
        followService.rejectFollowRequest(currentUser.getUserId(), requestId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/follow-requests/pending")
    public ResponseEntity<List<?>> getPendingFollowRequests(
            @AuthenticationPrincipal CurrentUser currentUser) {
        List<?> requests = followService.getPendingFollowRequests(currentUser.getUserId());
        return ResponseEntity.ok(requests);
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
     * Pretraga korisnika preko auth-service.
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResultDto>> search(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "") String q) {
        Long currentUserId = currentUser != null ? currentUser.getUserId() : null;
        return ResponseEntity.ok(followService.searchUsers(q, currentUserId));
    }
}