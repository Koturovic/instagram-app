package com.instagram.feed_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedResponseDTO {
    private Long postId;
    private Long userId;
    private String description;
    private Long likesCount;
    private List<Object> recentComments;
    private List<Object> mediaFiles;
}
