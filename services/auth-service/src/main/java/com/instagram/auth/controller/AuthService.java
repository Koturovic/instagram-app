package com.instagram.auth.controller;

import com.instagram.auth.User;
import com.instagram.auth.config.JwtService;
import com.instagram.auth.config.UserDetailsAdapter;
import com.instagram.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        // Provera da li email već postoji
        if (repository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email već postoji");
        }

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        repository.save(user);

        UserDetails userDetails = new UserDetailsAdapter(user);
        var jwtToken = jwtService.generateToken(userDetails);
        return AuthenticationResponse.builder().token(jwtToken).build();
    }

    public AuthenticationResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));
        var user = repository.findByEmail(request.getEmail()).orElseThrow();

        UserDetails userDetails = new UserDetailsAdapter(user);
        var jwtToken = jwtService.generateToken(userDetails);

        return AuthenticationResponse.builder().token(jwtToken).build();
    }
}
