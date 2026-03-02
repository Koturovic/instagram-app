package com.instagram.user_service.security;

import com.instagram.user_service.service.ProfileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls Auth service: validate token and get profile by userId.
 * GET {AUTH_SERVICE_URL}/api/v1/auth/validate
 * GET {AUTH_SERVICE_URL}/api/v1/auth/profiles/{userId}
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
     * Dohvata profil korisnika iz auth-service.
     * GET {AUTH_SERVICE_URL}/api/v1/auth/profiles/{userId}
     * Header: Authorization: Bearer <token>
     *
     * @param userId      auth userId
     * @param bearerToken token (without "Bearer " prefix) za autorizaciju poziva
     * @return AuthProfileResponse (userId, username, isPrivate)
     * @throws ProfileNotFoundException kada auth vrati 404
     */
    public AuthProfileResponse getProfileByUserId(Long userId, String bearerToken) {
        String url = authServiceProperties.getServiceUrl().replaceAll("/$", "") + PROFILES_PATH + "/" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
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
            throw new ProfileNotFoundException(userId);
        } catch (HttpClientErrorException e) {
            log.warn("Auth service returned {} for GET profile: {}", e.getStatusCode(), e.getMessage());
            throw new ProfileNotFoundException("Profile not found for user: " + userId);
        } catch (Exception e) {
            log.warn("Auth service GET profile failed: {}", e.getMessage());
            throw new ProfileNotFoundException("Profile not found for user: " + userId);
        }

        throw new ProfileNotFoundException(userId);
    }

    public static class TokenInvalidException extends RuntimeException {
        public TokenInvalidException(String message) {
            super(message);
        }
    }
}
