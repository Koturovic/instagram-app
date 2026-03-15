package com.instagram.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResultDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private Boolean isPrivate;
}