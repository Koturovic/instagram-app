package com.instagram.user_service.service;

import com.instagram.user_service.domain.Block;
import com.instagram.user_service.domain.Follow;
import com.instagram.user_service.domain.FollowRequest;
import com.instagram.user_service.domain.FollowRequest.FollowRequestStatus;
import com.instagram.user_service.dto.FollowRequestResponse;
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
            if (followRequestRepository.existsByRequesterUserIdAndTargetUserIdAndStatus(
                    currentUserId, targetUserId, FollowRequestStatus.PENDING)) {
                throw new BadRequestException("Follow request already pending");
            }
            FollowRequest req = followRequestRepository.save(FollowRequest.builder()
                    .requesterUserId(currentUserId)
                    .targetUserId(targetUserId)
                    .status(FollowRequestStatus.PENDING)
                    .build());
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
}
