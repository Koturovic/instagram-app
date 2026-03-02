package com.instagram.user_service.service;

import com.instagram.user_service.domain.Block;
import com.instagram.user_service.repository.BlockRepository;
import com.instagram.user_service.repository.FollowRepository;
import com.instagram.user_service.repository.FollowRequestRepository;
import com.instagram.user_service.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;

    /**
     * Blokira korisnika: insert Block, uklanja follow relacije u oba smera i zahteve.
     */
    @Transactional
    public void block(Long targetUserId, CurrentUser currentUser) {
        Long blockerUserId = currentUser.getUserId();
        if (blockerUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot block yourself");
        }

        if (blockRepository.existsByBlockerUserIdAndBlockedUserId(blockerUserId, targetUserId)) {
            return;
        }

        followRepository.findByFollowerUserIdAndFollowingUserId(blockerUserId, targetUserId)
                .ifPresent(followRepository::delete);
        followRepository.findByFollowerUserIdAndFollowingUserId(targetUserId, blockerUserId)
                .ifPresent(followRepository::delete);
        followRequestRepository.findByRequesterUserIdAndTargetUserId(blockerUserId, targetUserId)
                .ifPresent(followRequestRepository::delete);
        followRequestRepository.findByRequesterUserIdAndTargetUserId(targetUserId, blockerUserId)
                .ifPresent(followRequestRepository::delete);

        Block block = Block.builder()
                .blockerUserId(blockerUserId)
                .blockedUserId(targetUserId)
                .build();
        blockRepository.save(block);
    }

    /**
     * Odblokira: delete Block.
     */
    @Transactional
    public void unblock(Long targetUserId, CurrentUser currentUser) {
        Long blockerUserId = currentUser.getUserId();
        blockRepository.findByBlockerUserIdAndBlockedUserId(blockerUserId, targetUserId)
                .ifPresent(blockRepository::delete);
    }
}
