package com.instagram.user_service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.instagram.user_service.exception.ProfileNotFoundException;

/**
 * Calls Auth service to validate the token.
 * GET {AUTH_SERVICE_URL}/api/v1/auth/validate
 * Header: Authorization: Bearer <token>
 * 200 OK → { userId, email }
 * 401 → token invalid
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private static final String VALIDATE_PATH = "/api/v1/auth/validate";
    private static final String PROFILES_PATH = "/api/v1/auth/profiles";

    private final AuthServiceProperties authServiceProperties;
    private final RestTemplate restTemplate;

    /**
     * Validates token with Auth service. Returns userId and email on success.
     *
     * @param bearerToken the token (without "Bearer " prefix)
     * @return AuthValidateResponse with userId and email
     * @throws TokenInvalidException when Auth returns 401 or validation fails
     */
    public AuthValidateResponse validate(String bearerToken) {
        String url = authServiceProperties.getServiceUrl().replaceAll("/$", "") + VALIDATE_PATH;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<AuthValidateResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    AuthValidateResponse.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            log.debug("Auth service returned 401: token invalid");
            throw new TokenInvalidException("Token invalid");
        } catch (HttpClientErrorException e) {
            log.warn("Auth service returned {}: {}", e.getStatusCode(), e.getMessage());
            throw new TokenInvalidException("Token validation failed");
        } catch (Exception e) {
            log.warn("Auth service call failed: {}", e.getMessage());
            throw new TokenInvalidException("Token validation failed");
        }

        throw new TokenInvalidException("Token validation failed");
    }

    /**
     * Dohvata profil po userId od auth-service (za follow/block logiku).
     * GET {AUTH_SERVICE_URL}/api/v1/auth/profiles/{userId}
     *
     * @throws ProfileNotFoundException kada auth vrati 404
     */
    public AuthProfileResponse getProfileByUserId(Long userId) {
        String url = authServiceProperties.getServiceUrl().replaceAll("/$", "") + PROFILES_PATH + "/" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<AuthProfileResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    AuthProfileResponse.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Auth service returned 404 for profile userId={}", userId);
            throw new ProfileNotFoundException("Profile not found for userId: " + userId);
        } catch (HttpClientErrorException e) {
            log.warn("Auth service returned {} for profile {}: {}", e.getStatusCode(), userId, e.getMessage());
            throw new ProfileNotFoundException("Could not fetch profile");
        } catch (Exception e) {
            log.warn("Auth service call failed for profile {}: {}", userId, e.getMessage());
            throw new ProfileNotFoundException("Could not fetch profile");
        }
        throw new ProfileNotFoundException("Could not fetch profile");
    }

    public static class TokenInvalidException extends RuntimeException {
        public TokenInvalidException(String message) {
            super(message);
        }
    }
}
