package com.instagram.auth.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;


    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleEmailAlreadyExists")
    class HandleEmailAlreadyExists{

        @Test
        @DisplayName("Vraca 409 sa EMAIL_ALREADY_EXISTS")
        void returns409(){
            var ex = new EmailAlreadyExistsException("Email već postoji");
            ResponseEntity<Map<String,String>> response = handler.handleEmailAlreadyExists(ex);


            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("EMAIL_ALREADY_EXISTS");
            assertThat(response.getBody().get("message")).isEqualTo("Email već postoji");


        }
    }


    @Nested
    @DisplayName("handleUsernameAlreadyExists")
    class HandleUsernameAlreadyExists{


        @Test
        @DisplayName("vraca 409 sa USERNAME_ALREADY_EXISTS")

        void returns409WithCorrectBody(){
            var ex = new UsernameAlreadyExistsException("Username već postoji");

            ResponseEntity<Map<String, String>> response = handler.handleUsernameAlreadyExists(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("USERNAME_ALREADY_EXISTS");
            assertThat(response.getBody().get("message")).isEqualTo("Username već postoji");
        }
    }

    @Nested
    @DisplayName("handleBadCredentials")
    class HandleBadCredantials{
        @Test
        @DisplayName("vrac 401 sa BAD_CREDETIALS")

        void return401(){
            var ex = new BadCredentialsException("Bad credentials");

            ResponseEntity<Map<String, String>> response = handler.handleBadCredentials(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("BAD_CREDENTIALS");
            assertThat(response.getBody().get("message")).isEqualTo("Pogrešan email ili lozinka");

        }
    }

    @Nested
    @DisplayName("handleValidation")

    class HandleValidation{

        @Test
        @DisplayName(
                "vraca 400 sa VALIDATION_FAILED i mapom gresaka"
        )

        void return400(){
            var bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(
                    new FieldError("registerRequest", "email","Email mora biti validan"),
                    new FieldError("registerRequest", "password", "Lozinka mora imati najmanje 6 karaktera")

            ));


            var ex = new MethodArgumentNotValidException(null,bindingResult);

            ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("VALIDATION_FAILED");
            assertThat(response.getBody().get("message")).isEqualTo("Validacija neuspela");

            @SuppressWarnings("unchecked")
            Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
            assertThat(errors).containsEntry("email", "Email mora biti validan");
            assertThat(errors).containsEntry("password", "Lozinka mora imati najmanje 6 karaktera");

        }
    }
}
