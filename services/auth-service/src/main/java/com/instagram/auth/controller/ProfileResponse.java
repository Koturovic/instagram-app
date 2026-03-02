package com.instagram.auth.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    /** User ID (iz auth_db) – za user-service follow/block logiku */
    private Integer userId;
    private String username;
    private Boolean isPrivate;
}
