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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowService")
class FollowServiceTest {

    @Mock
    private AuthServiceClient authServiceClient;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private FollowRequestRepository followRequestRepository;
    @Mock
    private BlockRepository blockRepository;

    @InjectMocks
    private FollowService followService;

    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;

    @Nested
    @DisplayName("getFollowersCount / getFollowingCount")
    class CountTests {

        @Test
        void getFollowersCount_returnsCountFromRepository() {
            when(followRepository.countByFollowingUserId(USER_2)).thenReturn(10L);
            assertThat(followService.getFollowersCount(USER_2)).isEqualTo(10L);
            verify(followRepository).countByFollowingUserId(USER_2);
        }

        @Test
        void getFollowingCount_returnsCountFromRepository() {
            when(followRepository.countByFollowerUserId(USER_1)).thenReturn(5L);
            assertThat(followService.getFollowingCount(USER_1)).isEqualTo(5L);
            verify(followRepository).countByFollowerUserId(USER_1);
        }
    }

    @Nested
    @DisplayName("sendFollowRequest")
    class SendFollowRequestTests {

        @Test
        void whenSameUser_throwsBadRequest() {
            assertThatThrownBy(() -> followService.sendFollowRequest(USER_1, USER_1))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot follow yourself");
            verify(authServiceClient, never()).getProfileByUserId(any());
        }

        @Test
        void whenBlockBetween_throwsForbidden() {
            when(authServiceClient.getProfileByUserId(USER_2)).thenReturn(profile(USER_2, false));
            when(blockRepository.existsBlockBetween(USER_1, USER_2)).thenReturn(true);

            assertThatThrownBy(() -> followService.sendFollowRequest(USER_1, USER_2))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Cannot follow this user");
            verify(followRepository, never()).save(any());
        }

        @Test
        void whenAlreadyFollowing_throwsBadRequest() {
            when(authServiceClient.getProfileByUserId(USER_2)).thenReturn(profile(USER_2, false));
            when(blockRepository.existsBlockBetween(USER_1, USER_2)).thenReturn(false);
            when(followRepository.existsByFollowerUserIdAndFollowingUserId(USER_1, USER_2)).thenReturn(true);

            assertThatThrownBy(() -> followService.sendFollowRequest(USER_1, USER_2))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Already following");
            verify(followRepository, never()).save(any(Follow.class));
        }

        @Test
        void whenPublicProfile_createsFollowAndReturnsFollowedTrue() {
            when(authServiceClient.getProfileByUserId(USER_2)).thenReturn(profile(USER_2, false));
            when(blockRepository.existsBlockBetween(USER_1, USER_2)).thenReturn(false);
            when(followRepository.existsByFollowerUserIdAndFollowingUserId(USER_1, USER_2)).thenReturn(false);
            when(followRepository.save(any(Follow.class))).thenAnswer(i -> i.getArgument(0));

            FollowRequestResponse response = followService.sendFollowRequest(USER_1, USER_2);

            assertThat(response.getFollowed()).isTrue();
            assertThat(response.getRequestId()).isNull();
            verify(followRepository).save(any(Follow.class));
            verify(followRequestRepository, never()).save(any());
        }

        @Test
        void whenPrivateProfileAndNoPending_createsFollowRequestAndReturnsFollowedFalse() {
            when(authServiceClient.getProfileByUserId(USER_2)).thenReturn(profile(USER_2, true));
            when(blockRepository.existsBlockBetween(USER_1, USER_2)).thenReturn(false);
            when(followRepository.existsByFollowerUserIdAndFollowingUserId(USER_1, USER_2)).thenReturn(false);
            when(followRequestRepository.existsByRequesterUserIdAndTargetUserIdAndStatus(USER_1, USER_2, FollowRequestStatus.PENDING))
                    .thenReturn(false);
            FollowRequest saved = FollowRequest.builder().id(100L).requesterUserId(USER_1).targetUserId(USER_2).status(FollowRequestStatus.PENDING).build();
            when(followRequestRepository.save(any(FollowRequest.class))).thenReturn(saved);

            FollowRequestResponse response = followService.sendFollowRequest(USER_1, USER_2);

            assertThat(response.getFollowed()).isFalse();
            assertThat(response.getRequestId()).isEqualTo(100L);
            verify(followRequestRepository).save(any(FollowRequest.class));
            verify(followRepository, never()).save(any(Follow.class));
        }

        @Test
        void whenPrivateProfileAndPendingAlready_throwsBadRequest() {
            when(authServiceClient.getProfileByUserId(USER_2)).thenReturn(profile(USER_2, true));
            when(blockRepository.existsBlockBetween(USER_1, USER_2)).thenReturn(false);
            when(followRepository.existsByFollowerUserIdAndFollowingUserId(USER_1, USER_2)).thenReturn(false);
            when(followRequestRepository.existsByRequesterUserIdAndTargetUserIdAndStatus(USER_1, USER_2, FollowRequestStatus.PENDING))
                    .thenReturn(true);

            assertThatThrownBy(() -> followService.sendFollowRequest(USER_1, USER_2))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Follow request already pending");
            verify(followRequestRepository, never()).save(any());
        }

        @Test
        void whenProfileNotFound_throwsProfileNotFoundException() {
            when(authServiceClient.getProfileByUserId(USER_2)).thenThrow(new ProfileNotFoundException("not found"));

            assertThatThrownBy(() -> followService.sendFollowRequest(USER_1, USER_2))
                    .isInstanceOf(ProfileNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("acceptFollowRequest")
    class AcceptFollowRequestTests {

        @Test
        void whenRequestNotFound_throwsResourceNotFoundException() {
            when(followRequestRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> followService.acceptFollowRequest(USER_2, 100L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Follow request not found");
            verify(followRepository, never()).save(any());
        }

        @Test
        void whenCallerIsNotTarget_throwsForbidden() {
            FollowRequest req = request(100L, USER_1, USER_2, FollowRequestStatus.PENDING);
            when(followRequestRepository.findById(100L)).thenReturn(Optional.of(req));

            assertThatThrownBy(() -> followService.acceptFollowRequest(USER_1, 100L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Only the target user");
            verify(followRepository, never()).save(any());
        }

        @Test
        void whenRequestNotPending_throwsBadRequest() {
            FollowRequest req = request(100L, USER_1, USER_2, FollowRequestStatus.ACCEPTED);
            when(followRequestRepository.findById(100L)).thenReturn(Optional.of(req));

            assertThatThrownBy(() -> followService.acceptFollowRequest(USER_2, 100L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already processed");
        }

        @Test
        void whenTargetAccepts_savesAcceptedAndCreatesFollow() {
            FollowRequest req = request(100L, USER_1, USER_2, FollowRequestStatus.PENDING);
            when(followRequestRepository.findById(100L)).thenReturn(Optional.of(req));
            when(followRequestRepository.save(any(FollowRequest.class))).thenAnswer(i -> i.getArgument(0));
            when(followRepository.save(any(Follow.class))).thenAnswer(i -> i.getArgument(0));

            followService.acceptFollowRequest(USER_2, 100L);

            assertThat(req.getStatus()).isEqualTo(FollowRequestStatus.ACCEPTED);
            verify(followRequestRepository).save(req);
            verify(followRepository).save(any(Follow.class));
        }
    }

    @Nested
    @DisplayName("unfollow")
    class UnfollowTests {

        @Test
        void deletesFollowRelation() {
            followService.unfollow(USER_1, USER_2);
            verify(followRepository).deleteByFollowerUserIdAndFollowingUserId(USER_1, USER_2);
        }
    }

    @Nested
    @DisplayName("block")
    class BlockTests {

        @Test
        void whenBlockSelf_throwsBadRequest() {
            assertThatThrownBy(() -> followService.block(USER_1, USER_1))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot block yourself");
            verify(blockRepository, never()).save(any());
        }

        @Test
        void whenAlreadyBlocking_throwsBadRequest() {
            when(authServiceClient.getProfileByUserId(USER_2)).thenReturn(profile(USER_2, false));
            when(blockRepository.existsByBlockerUserIdAndBlockedUserId(USER_1, USER_2)).thenReturn(true);

            assertThatThrownBy(() -> followService.block(USER_1, USER_2))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Already blocking");
            verify(blockRepository, never()).save(any());
        }

        @Test
        void whenProfileNotFound_throwsProfileNotFoundException() {
            when(authServiceClient.getProfileByUserId(USER_2)).thenThrow(new ProfileNotFoundException("not found"));

            assertThatThrownBy(() -> followService.block(USER_1, USER_2))
                    .isInstanceOf(ProfileNotFoundException.class);
        }

        @Test
        void whenValid_removesFollowsBothDirectionsAndSavesBlock() {
            when(authServiceClient.getProfileByUserId(USER_2)).thenReturn(profile(USER_2, false));
            when(blockRepository.existsByBlockerUserIdAndBlockedUserId(USER_1, USER_2)).thenReturn(false);
            when(blockRepository.save(any(Block.class))).thenAnswer(i -> i.getArgument(0));

            followService.block(USER_1, USER_2);

            verify(followRepository).deleteByFollowerUserIdAndFollowingUserId(USER_1, USER_2);
            verify(followRepository).deleteByFollowerUserIdAndFollowingUserId(USER_2, USER_1);
            verify(blockRepository).save(any(Block.class));
        }
    }

    @Nested
    @DisplayName("unblock")
    class UnblockTests {

        @Test
        void deletesBlock() {
            followService.unblock(USER_1, USER_2);
            verify(blockRepository).deleteByBlockerUserIdAndBlockedUserId(USER_1, USER_2);
        }
    }

    private static AuthProfileResponse profile(Long userId, boolean isPrivate) {
        AuthProfileResponse p = new AuthProfileResponse();
        p.setUserId(userId);
        p.setUsername("user" + userId);
        p.setIsPrivate(isPrivate);
        return p;
    }

    private static FollowRequest request(Long id, Long requester, Long target, FollowRequestStatus status) {
        FollowRequest r = new FollowRequest();
        r.setId(id);
        r.setRequesterUserId(requester);
        r.setTargetUserId(target);
        r.setStatus(status);
        return r;
    }
}
