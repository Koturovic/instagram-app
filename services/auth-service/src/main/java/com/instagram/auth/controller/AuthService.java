package com.instagram.auth.controller;

import com.instagram.auth.Profile;
import com.instagram.auth.User;
import com.instagram.auth.config.JwtService;
import com.instagram.auth.config.UserDetailsAdapter;
import com.instagram.auth.exception.EmailAlreadyExistsException;
import com.instagram.auth.exception.ProfileNotFoundException;
import com.instagram.auth.exception.UsernameAlreadyExistsException;
import com.instagram.auth.repository.ProfileRepository;
import com.instagram.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email već postoji");
        }
        if (profileRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username već postoji");
        }

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userRepository.save(user);

        var profile = Profile.builder()
                .user(user)
                .username(request.getUsername())
                .isPrivate(false)
                .build();
        profileRepository.save(profile);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        UserDetails userDetails = new UserDetailsAdapter(user);
        var jwtToken = jwtService.generateToken(extraClaims, userDetails);
        return AuthenticationResponse.builder().token(jwtToken).build();
    }

    public AuthenticationResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        // dodajemo userId u token kao extra claim
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());

        UserDetails userDetails = new UserDetailsAdapter(user);
        var jwtToken = jwtService.generateToken(extraClaims, userDetails);

        return AuthenticationResponse.builder().token(jwtToken).build();
    }

    public ValidateResponse validateToken(String token) {
        String email = jwtService.extractUsername(token);
        var user = userRepository.findByEmail(email).orElseThrow();
        UserDetails userDetails = new UserDetailsAdapter(user);
        if (!jwtService.isTokenValid(token, userDetails)) {
            throw new RuntimeException("Token nije validan");
        }
        return ValidateResponse.builder()
                .userId(user.getId())
                .email(email)
                .build();
    }

    /**
     * Dohvata profil po userId (npr. user-service).
     * GET /api/v1/auth/profiles/{userId} → { userId, username, isPrivate }.
     */
    public ProfileResponse getProfileByUserId(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("User not found"));
        Profile profile = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));
        return ProfileResponse.builder()
                .userId(user.getId())
                .username(profile.getUsername())
                .isPrivate(profile.getIsPrivate())
                .build();
    }

    /**
     * Pretraga profila po username-u ili imenu/prezimenu (auth_db).
     * GET /api/v1/auth/profiles/search?q=...
     */
    public List<ProfileSearchResponse> searchProfiles(String q) {
        if (q == null || q.trim().isEmpty()) {
            return List.of();
        }
        return profileRepository.searchByUsernameOrName(q.trim()).stream()
                .map(p -> ProfileSearchResponse.builder()
                        .userId(p.getUser().getId())
                        .username(p.getUsername())
                        .firstName(p.getUser().getFirstName())
                        .lastName(p.getUser().getLastName())
                        .profileImageUrl(p.getProfileImageUrl())
                        .isPrivate(p.getIsPrivate())
                        .build())
                .collect(Collectors.toList());
    }
}
