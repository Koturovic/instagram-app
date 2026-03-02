package com.instagram.user_service.service;

/**
 * Baca se kada auth-service vrati 404 za GET /api/v1/auth/profiles/{userId}.
 */
public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(String message) {
        super(message);
    }

    public ProfileNotFoundException(Long userId) {
        super("Profile not found for user: " + userId);
    }
}
