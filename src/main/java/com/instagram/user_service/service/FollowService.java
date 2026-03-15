package com.instagram.user_service.service;

import com.instagram.user_service.domain.Block;
import com.instagram.user_service.domain.Follow;
import com.instagram.user_service.domain.FollowRequest;
import com.instagram.user_service.domain.FollowRequest.FollowRequestStatus;
import com.instagram.user_service.dto.FollowRequestResponse;
import com.instagram.user_service.dto.FollowUserDto;
import com.instagram.user_service.dto.PendingFollowRequestDto;
import com.instagram.user_service.dto.RelationshipStatusDto;
import com.instagram.user_service.dto.UserSearchResultDto;
import com.instagram.user_service.exception.ProfileNotFoundException;
import com.instagram.user_service.repository.BlockRepository;
import com.instagram.user_service.repository.FollowRepository;
import com.instagram.user_service.repository.FollowRequestRepository;
import com.instagram.user_service.security.AuthProfileResponse;
import com.instagram.user_service.security.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final AuthServiceClient authServiceClient;
    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final BlockRepository blockRepository;

    public long getFollowersCount(Long userId) {
        return followRepository.countByFollowingUserId(userId);
    }

    public long getFollowingCount(Long userId) {
        return followRepository.countByFollowerUserId(userId);
    }

    public List<FollowUserDto> getFollowers(Long userId) {
        List<Long> followerIds = followRepository.findByFollowingUserId(userId)
                .stream()
                .map(Follow::getFollowerUserId)
                .collect(Collectors.toList());

        List<FollowUserDto> result = new ArrayList<>();
        for (Long followerId : followerIds) {
            try {
                AuthProfileResponse profile = authServiceClient.getProfileByUserId(followerId);
                result.add(FollowUserDto.builder()
                        .userId(profile.getUserId())
                        .username(profile.getUsername())
                        .profileImageUrl(profile.getProfileImageUrl())
                        .build());
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public List<FollowUserDto> getFollowing(Long userId) {
        List<Long> followingIds = followRepository.findByFollowerUserId(userId)
                .stream()
                .map(Follow::getFollowingUserId)
                .collect(Collectors.toList());

        List<FollowUserDto> result = new ArrayList<>();
        for (Long followingId : followingIds) {
            try {
                AuthProfileResponse profile = authServiceClient.getProfileByUserId(followingId);
                result.add(FollowUserDto.builder()
                        .userId(profile.getUserId())
                        .username(profile.getUsername())
                        .profileImageUrl(profile.getProfileImageUrl())
                        .build());
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public List<UserSearchResultDto> searchUsers(String q, Long currentUserId) {
        List<UserSearchResultDto> results = authServiceClient.searchProfiles(q);
        if (currentUserId == null) {
            return results;
        }

        return results.stream()
                .filter(result -> result.getId() != null)
                .filter(result -> !currentUserId.equals(result.getId()))
                // Ako je target blokirao current user-a, sakrij iz rezultata.
                .filter(result -> !blockRepository.existsByBlockerUserIdAndBlockedUserId(result.getId(), currentUserId))
                // Ako je current user blokirao target, ipak prikaži rezultat uz "Unblock" stanje.
                .map(result -> {
                    boolean blockedByCurrentUser = blockRepository
                            .existsByBlockerUserIdAndBlockedUserId(currentUserId, result.getId());
                    result.setBlockedByCurrentUser(blockedByCurrentUser);
                    return result;
                })
                .collect(Collectors.toList());
    }

    public RelationshipStatusDto getRelationshipStatus(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || targetUserId == null || currentUserId.equals(targetUserId)) {
            return RelationshipStatusDto.builder()
                .following(false)
                .pending(false)
                .blocked(false)
                .build();
        }

        boolean following = followRepository.existsByFollowerUserIdAndFollowingUserId(currentUserId, targetUserId);
        boolean pending = followRequestRepository.existsByRequesterUserIdAndTargetUserIdAndStatus(
            currentUserId, targetUserId, FollowRequestStatus.PENDING);
        boolean blocked = blockRepository.existsBlockBetween(currentUserId, targetUserId);

        return RelationshipStatusDto.builder()
            .following(following)
            .pending(pending)
            .blocked(blocked)
            .build();
    }

    /**
     * Korisnik currentUserId šalje zahtev da prati targetUserId.
     * Ako je target javni → odmah kreira Follow. Ako je privatni → kreira FollowRequest (PENDING).
     */
    @Transactional
    public FollowRequestResponse sendFollowRequest(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot follow yourself");
        }

        AuthProfileResponse targetProfile = authServiceClient.getProfileByUserId(targetUserId);

        if (blockRepository.existsBlockBetween(currentUserId, targetUserId)) {
            throw new ForbiddenException("Cannot follow this user");
        }

        if (followRepository.existsByFollowerUserIdAndFollowingUserId(currentUserId, targetUserId)) {
            throw new BadRequestException("Already following this user");
        }

        if (Boolean.TRUE.equals(targetProfile.getIsPrivate())) {
            FollowRequest req = followRequestRepository
                    .findByRequesterUserIdAndTargetUserId(currentUserId, targetUserId)
                    .map(existing -> {
                        if (existing.getStatus() == FollowRequestStatus.PENDING) {
                            throw new BadRequestException("Follow request already pending");
                        }
                        // Re-activate previously processed request to avoid unique-constraint insert failure.
                        existing.setStatus(FollowRequestStatus.PENDING);
                        return followRequestRepository.save(existing);
                    })
                    .orElseGet(() -> followRequestRepository.save(FollowRequest.builder()
                            .requesterUserId(currentUserId)
                            .targetUserId(targetUserId)
                            .status(FollowRequestStatus.PENDING)
                            .build()));
            return FollowRequestResponse.builder().requestId(req.getId()).followed(false).build();
        } else {
            followRepository.save(Follow.builder()
                    .followerUserId(currentUserId)
                    .followingUserId(targetUserId)
                    .build());
            return FollowRequestResponse.builder().requestId(null).followed(true).build();
        }
    }

    /**
     * Samo target (onaj kome je upućen zahtev) može da prihvati. requestId = id FollowRequest.
     */
    @Transactional
    public void acceptFollowRequest(Long currentUserId, Long requestId) {
        FollowRequest req = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow request not found"));

        if (!req.getTargetUserId().equals(currentUserId)) {
            throw new ForbiddenException("Only the target user can accept this request");
        }
        if (req.getStatus() != FollowRequestStatus.PENDING) {
            throw new BadRequestException("Request already processed");
        }

        req.setStatus(FollowRequestStatus.ACCEPTED);
        followRequestRepository.save(req);
        followRepository.save(Follow.builder()
                .followerUserId(req.getRequesterUserId())
                .followingUserId(req.getTargetUserId())
                .build());
    }

    @Transactional
    public void removeFollower(Long currentUserId, Long followerUserId) {
        followRepository.deleteByFollowerUserIdAndFollowingUserId(followerUserId, currentUserId);
    }

    @Transactional
    public void unfollow(Long currentUserId, Long targetUserId) {
        followRepository.deleteByFollowerUserIdAndFollowingUserId(currentUserId, targetUserId);
    }

    /**
     * Blokira targetUserId. Ako postoji follow relacija u oba smera, briše je.
     */
    @Transactional
    public void block(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot block yourself");
        }
        authServiceClient.getProfileByUserId(targetUserId); // 404 ako ne postoji

        if (blockRepository.existsByBlockerUserIdAndBlockedUserId(currentUserId, targetUserId)) {
            throw new BadRequestException("Already blocking this user");
        }

        followRepository.deleteByFollowerUserIdAndFollowingUserId(currentUserId, targetUserId);
        followRepository.deleteByFollowerUserIdAndFollowingUserId(targetUserId, currentUserId);
        blockRepository.save(Block.builder()
                .blockerUserId(currentUserId)
                .blockedUserId(targetUserId)
                .build());
    }

    @Transactional
    public void unblock(Long currentUserId, Long targetUserId) {
        blockRepository.deleteByBlockerUserIdAndBlockedUserId(currentUserId, targetUserId);
    }

    /**
     * Pronađi sve pending follow zahteve za currentUserId (gde je on target).
     * Vraća info sa requester podacima (username, avatar).
     */
    public List<PendingFollowRequestDto> getPendingFollowRequests(Long userId) {
        return followRequestRepository
                .findByTargetUserIdAndStatus(userId, FollowRequestStatus.PENDING)
                .stream()
                .map(req -> {
                    AuthProfileResponse requesterProfile = authServiceClient.getProfileByUserId(req.getRequesterUserId());
                    return PendingFollowRequestDto.builder()
                            .requestId(req.getId())
                            .requesterUserId(req.getRequesterUserId())
                            .requesterUsername(requesterProfile.getUsername())
                            .requesterProfileImage(requesterProfile.getProfileImageUrl())
                            .createdAt(req.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Odbij follow zahtev. Samo target (onaj kome je upućen zahtev) može da odbije.
     */
    @Transactional
    public void rejectFollowRequest(Long currentUserId, Long requestId) {
        FollowRequest req = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow request not found"));

        if (!req.getTargetUserId().equals(currentUserId)) {
            throw new ForbiddenException("Only the target user can reject this request");
        }
        if (req.getStatus() != FollowRequestStatus.PENDING) {
            throw new BadRequestException("Request already processed");
        }

        req.setStatus(FollowRequestStatus.REJECTED);
        followRequestRepository.save(req);
    }
}