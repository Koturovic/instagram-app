package com.instagram.user_service.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "app.auth")
@Validated
@Getter
@Setter
public class AuthServiceProperties {

    /**
     * Base URL of Auth service (no trailing slash).
     * Local: http://localhost:8080
     * Docker: http://auth-service:8080
     */
    @NotBlank(message = "app.auth.service-url is required")
    private String serviceUrl = "http://localhost:8080";
}
