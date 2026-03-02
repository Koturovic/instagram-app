package com.instagram.user_service.repository;

import com.instagram.user_service.domain.FollowRequest;
import com.instagram.user_service.domain.FollowRequest.FollowRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {

    Optional<FollowRequest> findByRequesterUserIdAndTargetUserIdAndStatus(
            Long requesterUserId, Long targetUserId, FollowRequestStatus status);

    List<FollowRequest> findByTargetUserIdAndStatus(Long targetUserId, FollowRequestStatus status);

    Optional<FollowRequest> findByIdAndTargetUserId(Long id, Long targetUserId);

    boolean existsByRequesterUserIdAndTargetUserIdAndStatus(
            Long requesterUserId, Long targetUserId, FollowRequestStatus status);

    Optional<FollowRequest> findByRequesterUserIdAndTargetUserId(Long requesterUserId, Long targetUserId);
}
