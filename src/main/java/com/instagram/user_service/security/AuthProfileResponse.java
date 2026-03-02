package com.instagram.user_service.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirror odgovora auth-service GET /api/v1/auth/profiles/{userId}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthProfileResponse {

    private Long userId;
    private String username;
    private Boolean isPrivate;
}
