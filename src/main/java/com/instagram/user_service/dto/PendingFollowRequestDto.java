package com.instagram.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingFollowRequestDto {
    private Long requestId;
    private Long requesterUserId;
    private String requesterUsername;
    private String requesterProfileImage;
    private Instant createdAt;
}