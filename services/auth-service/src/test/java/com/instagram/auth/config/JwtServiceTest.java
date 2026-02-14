package com.instagram.auth.config;

import com.instagram.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import io.jsonwebtoken.ExpiredJwtException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String JWT_SECRET = "78ce5c76e64a0982fd941ebf1563a5016e1fdab51d8cbea376771b52138a7cf0";
    private static final long JWT_EXPIRATION = 3600000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", JWT_EXPIRATION);
    }

    private UserDetails userDetails(String email) {
        User user = User.builder()
                .id(1)
                .email(email)
                .firstName("Ana")
                .lastName("Ivanovic")
                .password("encoded")
                .createdAt(Instant.now())
                .build();
        return new UserDetailsAdapter(user);
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("vraća ne-prazan token")
        void returnsNonEmptyToken() {
            UserDetails userDetails = userDetails("ana@test.com");

            String token = jwtService.generateToken(userDetails);

            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("vraća različite tokene za različite korisnike")
        void returnsDifferentTokensForDifferentUsers() {
            String token1 = jwtService.generateToken(userDetails("user1@test.com"));
            String token2 = jwtService.generateToken(userDetails("user2@test.com"));

            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("vraća token sa extra claims")
        void withExtraClaims_returnsToken() {
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("custom", "value");
            UserDetails userDetails = userDetails("ana@test.com");

            String token = jwtService.generateToken(extraClaims, userDetails);

            assertThat(token).isNotBlank();
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("izvlaci username (email) iz tokena")
        void extractsUsernameFromToken() {
            UserDetails userDetails = userDetails("ana@test.com");
            String token = jwtService.generateToken(userDetails);

            String username = jwtService.extractUsername(token);

            assertThat(username).isEqualTo("ana@test.com");
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("vraća true kada token validan i username odgovara")
        void validTokenAndMatchingUsername_returnsTrue() {
            UserDetails userDetails = userDetails("ana@test.com");
            String token = jwtService.generateToken(userDetails);

            boolean valid = jwtService.isTokenValid(token, userDetails);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("vraća false kada username ne odgovara")
        void differentUsername_returnsFalse() {
            UserDetails userDetails = userDetails("ana@test.com");
            String token = jwtService.generateToken(userDetails);
            UserDetails otherUser = userDetails("drugi@test.com");

            boolean valid = jwtService.isTokenValid(token, otherUser);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("baca ExpiredJwtException kada je token istekao")
        void expiredToken_throwsExpiredJwtException() {
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", -3600000L);
            UserDetails userDetails = userDetails("ana@test.com");
            String token = jwtService.generateToken(userDetails);
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", JWT_EXPIRATION);

            assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }
}
