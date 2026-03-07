package com.instagram.user_service.exception;

import com.instagram.user_service.service.BadRequestException;
import com.instagram.user_service.service.ForbiddenException;
import com.instagram.user_service.service.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

    @Test
    void handleNotFound_returns404AndMessage() {
        ResourceNotFoundException e = new ResourceNotFoundException("Not found");
        ResponseEntity<Map<String, String>> r = handler.handleNotFound(e);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).containsEntry("message", "Not found");
    }

    @Test
    void handleProfileNotFound_returns404AndMessage() {
        ProfileNotFoundException e = new ProfileNotFoundException("Profile not found");
        ResponseEntity<Map<String, String>> r = handler.handleProfileNotFound(e);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).containsEntry("message", "Profile not found");
    }

    @Test
    void handleForbidden_returns403AndMessage() {
        ForbiddenException e = new ForbiddenException("Forbidden");
        ResponseEntity<Map<String, String>> r = handler.handleForbidden(e);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r.getBody()).containsEntry("message", "Forbidden");
    }

    @Test
    void handleBadRequest_returns400AndMessage() {
        BadRequestException e = new BadRequestException("Bad request");
        ResponseEntity<Map<String, String>> r = handler.handleBadRequest(e);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("message", "Bad request");
    }

    @Test
    void handleUnauthorized_returns401AndFixedMessage() {
        AuthenticationException e = mock(AuthenticationException.class);
        when(e.getMessage()).thenReturn("bad token");
        ResponseEntity<Map<String, String>> r = handler.handleUnauthorized(e);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody()).containsEntry("message", "Unauthorized");
    }

    @Test
    void handleValidation_returns400WithErrorsMap() {
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(e.getBindingResult()).thenReturn(bindingResult);
        FieldError fieldError = new FieldError("request", "username", "must not be blank");
        when(bindingResult.getAllErrors()).thenReturn(java.util.List.of(fieldError));

        ResponseEntity<Map<String, Object>> r = handler.handleValidation(e);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsKey("message");
        assertThat(r.getBody().get("message")).isEqualTo("Validation failed");
        assertThat(r.getBody()).containsKey("errors");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) r.getBody().get("errors");
        assertThat(errors).containsEntry("username", "must not be blank");
    }
}
