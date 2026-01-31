package com.instagram.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // globalni exception handler za sve exceptione
public class GlobalExceptionHandler {

    // hendluje exception kada email vec koristi neko drugi
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "EMAIL_ALREADY_EXISTS",
                        "message", ex.getMessage()));

        // {
        // "error": "EMAIL_ALREADY_EXISTS",
        // "message": "Email već postoji"
        // }
    }

    // hendluje exception kada username vec koristi neko drugi
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "USERNAME_ALREADY_EXISTS",
                        "message", ex.getMessage()));
    }

    // hendluje exception kada email ili lozinka nisu ispravni
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "BAD_CREDENTIALS",
                        "message", "Pogrešan email ili lozinka"));
    }

    // hendluje exception kada validacija ne uspe

    // baca se kada @Valid ne prođe (npr. @NotBlank, @Email).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("error", "VALIDATION_FAILED");
        body.put("message", "Validacija neuspela");
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}

// EmailAlreadyExistsException -> 409 Conflict email već postoji
// UsernameAlreadyExistsException -> 409 Conflict username već postoji
// BadCredentialsException -> 401 Unauthorized pogrešan email/lozinka
// MethodArgumentNotValidException -> 400 Bad Request validacija DTO-a
