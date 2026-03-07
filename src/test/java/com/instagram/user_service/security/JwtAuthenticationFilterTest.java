package com.instagram.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private AuthServiceClient authServiceClient;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new JwtAuthenticationFilter(authServiceClient);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter_actuatorPath_returnsTrue() {
        when(request.getRequestURI()).thenReturn("/actuator/health");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_errorPath_returnsTrue() {
        when(request.getRequestURI()).thenReturn("/error");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_devPath_returnsTrue() {
        when(request.getRequestURI()).thenReturn("/dev/test-token");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_apiPath_returnsFalse() {
        when(request.getRequestURI()).thenReturn("/api/v1/users/1/followers/count");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void doFilterInternal_whenNoAuthorizationHeader_sends401AndDoesNotContinue() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/users/1/followers/count");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertThat(responseWriter.toString()).contains("Missing or invalid");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_whenInvalidBearer_sends401AndDoesNotContinue() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/users/1/followers/count");
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_whenTokenInvalidException_sends401AndDoesNotContinue() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/users/1/followers/count");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(authServiceClient.validate("bad-token")).thenThrow(new AuthServiceClient.TokenInvalidException("invalid"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(responseWriter.toString()).contains("Unauthorized");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_whenValidToken_setsAuthenticationAndContinues() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/users/1/followers/count");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        AuthValidateResponse validateResponse = new AuthValidateResponse(1L, "user@test.com");
        when(authServiceClient.validate("valid-token")).thenReturn(validateResponse);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOf(CurrentUser.class);
        CurrentUser principal = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("user@test.com");
    }

    @Test
    void doFilterInternal_whenValidateReturnsNullUserId_sends401() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/users/1/followers/count");
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(authServiceClient.validate("token")).thenReturn(new AuthValidateResponse(null, "e@mail.com"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
