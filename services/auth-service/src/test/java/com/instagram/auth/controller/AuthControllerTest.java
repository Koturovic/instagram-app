package com.instagram.auth.controller;

import com.instagram.auth.exception.EmailAlreadyExistsException;
import com.instagram.auth.exception.GlobalExceptionHandler;
import com.instagram.auth.exception.UsernameAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)  //Omogućava Mockito integraciju sa JUnit 5.
@DisplayName("AuthController")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String VALID_REGISTER_JSON = """
            {"firstName":"Ana","lastName":"Ivanovic","username":"anaivan","email":"ana@test.com","password":"lozinka123"}
            """;

    private static final String VALID_LOGIN_JSON = """
            {"email":"ana@test.com","password":"lozinka123"}
            """;

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("vraća 200 i token kada su podaci validni")
        void success_returns200AndToken() throws Exception {
            when(authService.register(any())).thenReturn(
                    AuthenticationResponse.builder().token("jwt-token-123").build()
            );

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REGISTER_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token-123"));
        }

        @Test
        @DisplayName("vraća 409 kada email već postoji")
        void emailExists_returns409() throws Exception {
            when(authService.register(any()))
                    .thenThrow(new EmailAlreadyExistsException("Email već postoji"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REGISTER_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.message").value("Email već postoji"));
        }

        @Test
        @DisplayName("vraća 409 kada username već postoji")
        void usernameExists_returns409() throws Exception {
            when(authService.register(any()))
                    .thenThrow(new UsernameAlreadyExistsException("Username već postoji"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REGISTER_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("USERNAME_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.message").value("Username već postoji"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("vraća 200 i token kada su kredencijali ispravni")
        void success_returns200AndToken() throws Exception {
            when(authService.login(any())).thenReturn(
                    AuthenticationResponse.builder().token("jwt-login-token").build()
            );

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_LOGIN_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-login-token"));
        }

        @Test
        @DisplayName("vraća 401 kada su kredencijali pogrešni")
        void badCredentials_returns401() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_LOGIN_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("BAD_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("Pogrešan email ili lozinka"));
        }
    }
}
