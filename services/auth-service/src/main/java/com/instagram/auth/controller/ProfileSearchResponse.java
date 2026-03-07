package com.instagram.auth.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Jedan rezultat pretrage profila (auth_db).
 * Frontend očekuje id, username, firstName, lastName, profileImageUrl, isPrivate.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileSearchResponse {
    private Integer userId;
    private String username;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private Boolean isPrivate;

    /** Za kompatibilnost sa frontendom koji koristi user.id */
    @JsonProperty("id")
    public Integer getId() {
        return userId;
    }
}
