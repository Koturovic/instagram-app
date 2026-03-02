package com.instagram.user_service.service;

import com.instagram.user_service.domain.Follow;
import com.instagram.user_service.domain.FollowRequest;
import com.instagram.user_service.repository.BlockRepository;
import com.instagram.user_service.repository.FollowRepository;
import com.instagram.user_service.repository.FollowRequestRepository;
import com.instagram.user_service.security.AuthProfileResponse;
import com.instagram.user_service.security.AuthServiceClient;
import com.instagram.user_service.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final BlockRepository blockRepository;
    private final AuthServiceClient authServiceClient;

    /**
     * Slanje zahteva za praćenje ili odmah follow (ako je target javni).
     * Koristi getProfileByUserId da proveri da li je target privatni → FollowRequest PENDING, ili javni → Follow.
     *
     * @return requestId (FollowRequest.id ili Follow.id) za accept endpoint
     */
    @Transactional
    public Long sendFollowRequest(Long targetUserId, CurrentUser currentUser, String bearerToken) {
        Long requesterUserId = currentUser.getUserId();
        if (requesterUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot follow yourself");
        }

        if (blockRepository.existsBlockBetween(requesterUserId, targetUserId)) {
            throw new ForbiddenException("Cannot follow this user");
        }

        AuthProfileResponse targetProfile = authServiceClient.getProfileByUserId(targetUserId, bearerToken);
        boolean targetIsPrivate = Boolean.TRUE.equals(targetProfile.getIsPrivate());

        if (followRepository.existsByFollowerUserIdAndFollowingUserId(requesterUserId, targetUserId)) {
            return followRepository.findByFollowerUserIdAndFollowingUserId(requesterUserId, targetUserId)
                    .orElseThrow()
                    .getId();
        }

        if (targetIsPrivate) {
            FollowRequest existing = followRequestRepository.findByRequesterUserIdAndTargetUserId(requesterUserId, targetUserId)
                    .orElse(null);
            if (existing != null) {
                return existing.getId();
            }
            FollowRequest request = FollowRequest.builder()
                    .requesterUserId(requesterUserId)
                    .targetUserId(targetUserId)
                    .status(FollowRequest.FollowRequestStatus.PENDING)
                    .build();
            request = followRequestRepository.save(request);
            return request.getId();
        } else {
            Follow follow = Follow.builder()
                    .followerUserId(requesterUserId)
                    .followingUserId(targetUserId)
                    .build();
            follow = followRepository.save(follow);
            return follow.getId();
        }
    }

    /**
     * Target prihvata zahtev → update FollowRequest na ACCEPTED, kreira Follow.
     * Ako je requestId = Follow.id (javni), no-op.
     */
    @Transactional
    public void acceptFollowRequest(Long requestId, CurrentUser currentUser) {
        Long targetUserId = currentUser.getUserId();

        FollowRequest followRequest = followRequestRepository.findById(requestId).orElse(null);
        if (followRequest != null) {
            if (followRequest.getStatus() != FollowRequest.FollowRequestStatus.PENDING) {
                return;
            }
            if (!followRequest.getTargetUserId().equals(targetUserId)) {
                throw new ForbiddenException("Only the target user can accept this request");
            }
            followRequest.setStatus(FollowRequest.FollowRequestStatus.ACCEPTED);
            followRequestRepository.save(followRequest);
            if (!followRepository.existsByFollowerUserIdAndFollowingUserId(followRequest.getRequesterUserId(), followRequest.getTargetUserId())) {
                Follow follow = Follow.builder()
                        .followerUserId(followRequest.getRequesterUserId())
                        .followingUserId(followRequest.getTargetUserId())
                        .build();
                followRepository.save(follow);
            }
            return;
        }

        Follow follow = followRepository.findById(requestId).orElse(null);
        if (follow != null) {
            return;
        }

        throw new ResourceNotFoundException("Follow request not found: " + requestId);
    }

    /**
     * Otpraćivanje: brisanje iz Follow i eventualno iz FollowRequest (otkazivanje zahteva).
     */
    @Transactional
    public void unfollow(Long targetUserId, CurrentUser currentUser) {
        Long requesterUserId = currentUser.getUserId();
        followRepository.findByFollowerUserIdAndFollowingUserId(requesterUserId, targetUserId)
                .ifPresent(followRepository::delete);
        followRequestRepository.findByRequesterUserIdAndTargetUserId(requesterUserId, targetUserId)
                .ifPresent(followRequestRepository::delete);
    }

    public long getFollowersCount(Long userId) {
        return followRepository.findByFollowingUserId(userId).size();
    }

    public long getFollowingCount(Long userId) {
        return followRepository.findByFollowerUserId(userId).size();
    }
}
