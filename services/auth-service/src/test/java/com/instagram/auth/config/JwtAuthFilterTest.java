package com.instagram.auth.config;

import com.instagram.auth.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter")
class JwtAuthFilterTest {



//   Client
//   ↓
//   Security Filter Chain
//   ↓
//  JWT Filter
//   ↓
//  Spring Security
//  ↓
//   Controller
//  ↓
//  Service
//  ↓
//  Database


    // spring security JWT arhitektuta: Request → JwtAuthFilter → SecurityContext → Controller

    @Mock
    private JwtService jwtService;

    @Mock
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private JwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    } //auth ostaje između testova da ovo nismo koristili

    @Nested
    @DisplayName("bez Authorization headera")
    class WithoutAuthHeader {

        @Test
        @DisplayName("poziva filterChain i ne postavlja autentifikaciju")
        void doesNotSetAuthentication() throws ServletException, IOException {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtService, never()).extractUsername(anyString());
            verify(userDetailsService, never()).loadUserByUsername(anyString());
        }
    }

    @Nested
    @DisplayName("sa pogresnim Authorization headerom")
    class WithInvalidAuthHeader {

        @Test
        @DisplayName("Authorization bez Bearer prefiksa - ne postavlja autentifikaciju")
        void invalidFormat_doesNotSetAuthentication() throws ServletException, IOException {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Basic abc123");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtService, never()).extractUsername(anyString());
        }
    }

    @Nested
    @DisplayName("sa validnim Bearer tokenom")
    class WithValidBearerToken {

        @Test
        @DisplayName("postavlja autentifikaciju u SecurityContext")
        void setsAuthenticationInContext() throws ServletException, IOException {
            String token = "valid-jwt-token";
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            UserDetails userDetails = new UserDetailsAdapter(User.builder()
                    .id(1)
                    .email("ana@test.com")
                    .firstName("Ana")
                    .lastName("Ivanovic")
                    .password("encoded")
                    .createdAt(Instant.now())
                    .build());

            when(jwtService.extractUsername(token)).thenReturn("ana@test.com");
            when(userDetailsService.loadUserByUsername("ana@test.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid(eq(token), eq(userDetails))).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.isAuthenticated()).isTrue();
            assertThat(auth.getPrincipal()).isEqualTo(userDetails);
            assertThat(auth.getAuthorities()).isNotEmpty();
        }

        @Test
        @DisplayName("ne postavlja autentifikaciju kada token nije validan")
        void invalidToken_doesNotSetAuthentication() throws ServletException, IOException {
            String token = "invalid-token";
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            UserDetails userDetails = new UserDetailsAdapter(User.builder()
                    .id(1)
                    .email("ana@test.com")
                    .build());

            when(jwtService.extractUsername(token)).thenReturn("ana@test.com");
            when(userDetailsService.loadUserByUsername("ana@test.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid(eq(token), eq(userDetails))).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("ne postavlja autentifikaciju kada SecurityContext vec ima auth")
        void existingAuthentication_notOverwritten() throws ServletException, IOException {
            String token = "valid-jwt-token";
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            UserDetails existingUser = new UserDetailsAdapter(User.builder()
                    .id(99)
                    .email("existing@test.com")
                    .build());
            SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            existingUser, null, existingUser.getAuthorities()));

            filter.doFilterInternal(request, response, filterChain);

            verify(userDetailsService, never()).loadUserByUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .isEqualTo(existingUser);
        }
    }
}
