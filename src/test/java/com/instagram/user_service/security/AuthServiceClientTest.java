package com.instagram.user_service.security;

import com.instagram.user_service.exception.ProfileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceClient")
class AuthServiceClientTest {

    @Mock
    private AuthServiceProperties authServiceProperties;
    @Mock
    private RestTemplate restTemplate;

    private AuthServiceClient client;

    @BeforeEach
    void setUp() {
        when(authServiceProperties.getServiceUrl()).thenReturn("http://auth:8080");
        client = new AuthServiceClient(authServiceProperties, restTemplate);
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        void when200AndBody_returnsAuthValidateResponse() {
            AuthValidateResponse body = new AuthValidateResponse(1L, "a@b.com");
            when(restTemplate.exchange(
                    eq("http://auth:8080/api/v1/auth/validate"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(AuthValidateResponse.class)
            )).thenReturn(ResponseEntity.ok(body));

            AuthValidateResponse result = client.validate("token123");
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("a@b.com");
        }

        @Test
        void whenServiceUrlHasTrailingSlash_stripsIt() {
            when(authServiceProperties.getServiceUrl()).thenReturn("http://auth:8080/");
            AuthValidateResponse body = new AuthValidateResponse(2L, "x@y.com");
            when(restTemplate.exchange(
                    eq("http://auth:8080/api/v1/auth/validate"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(AuthValidateResponse.class)
            )).thenReturn(ResponseEntity.ok(body));

            AuthValidateResponse result = client.validate("t");
            assertThat(result.getUserId()).isEqualTo(2L);
        }

        @Test
        void when401_throwsTokenInvalidException() {
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(AuthValidateResponse.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

            assertThatThrownBy(() -> client.validate("bad"))
                    .isInstanceOf(AuthServiceClient.TokenInvalidException.class)
                    .hasMessageContaining("Token validation failed");
        }

        @Test
        void whenOtherHttpClientError_throwsTokenInvalidException() {
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(AuthValidateResponse.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

            assertThatThrownBy(() -> client.validate("t"))
                    .isInstanceOf(AuthServiceClient.TokenInvalidException.class)
                    .hasMessageContaining("Token validation failed");
        }

        @Test
        void whenGenericException_throwsTokenInvalidException() {
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(AuthValidateResponse.class)))
                    .thenThrow(new RuntimeException("network error"));

            assertThatThrownBy(() -> client.validate("t"))
                    .isInstanceOf(AuthServiceClient.TokenInvalidException.class);
        }
    }

    @Nested
    @DisplayName("getProfileByUserId")
    class GetProfileByUserId {

        @Test
        void when200AndBody_returnsAuthProfileResponse() {
            AuthProfileResponse body = new AuthProfileResponse(10L, "user10", false, null);
            when(restTemplate.exchange(
                    eq("http://auth:8080/api/v1/auth/profiles/10"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(AuthProfileResponse.class)
            )).thenReturn(ResponseEntity.ok(body));

            AuthProfileResponse result = client.getProfileByUserId(10L);
            assertThat(result.getUserId()).isEqualTo(10L);
            assertThat(result.getUsername()).isEqualTo("user10");
            assertThat(result.getIsPrivate()).isFalse();
        }

        @Test
        void when404_throwsProfileNotFoundException() {
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(AuthProfileResponse.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> client.getProfileByUserId(999L))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessageContaining("Could not fetch profile");
        }

        @Test
        void whenOtherHttpClientError_throwsProfileNotFoundException() {
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(AuthProfileResponse.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

            assertThatThrownBy(() -> client.getProfileByUserId(1L))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessageContaining("Could not fetch profile");
        }

        @Test
        void whenGenericException_throwsProfileNotFoundException() {
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(AuthProfileResponse.class)))
                    .thenThrow(new RuntimeException("timeout"));

            assertThatThrownBy(() -> client.getProfileByUserId(1L))
                    .isInstanceOf(ProfileNotFoundException.class);
        }
    }
}
