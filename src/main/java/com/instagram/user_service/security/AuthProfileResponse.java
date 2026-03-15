package com.instagram.user_service.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthProfileResponse {

    private Long userId;   // auth vraća Integer, Jackson mapira u Long
    private String username;
    private Boolean isPrivate;
    private String profileImageUrl;
}