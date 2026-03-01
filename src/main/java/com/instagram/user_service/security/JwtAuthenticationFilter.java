package com.instagram.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates token via Auth service: GET {AUTH_SERVICE_URL}/api/v1/auth/validate
 * Header: Authorization: Bearer <token> (same as received from client)
 * 200 OK → body { userId, email } → set CurrentUser, continue
 * 401 → return 401 to client
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthServiceClient authServiceClient;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.startsWith("/error") || path.startsWith("/dev/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractToken(request);

        if (!StringUtils.hasText(token)) {
            send401(response, "Missing or invalid Authorization header");
            return;
        }

        try {
            AuthValidateResponse validateResponse = authServiceClient.validate(token);
            if (validateResponse == null || validateResponse.getUserId() == null) {
                send401(response, "Invalid token");
                return;
            }
            // username u CurrentUser koristimo za email (Auth vraća userId i email)
            String email = validateResponse.getEmail() != null ? validateResponse.getEmail() : "";
            CurrentUser principal = new CurrentUser(validateResponse.getUserId(), email);
            var auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } catch (AuthServiceClient.TokenInvalidException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            send401(response, "Unauthorized");
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7).trim();
        }
        return null;
    }

    private void send401(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"message\":\"" + escaped + "\"}");
    }
}
