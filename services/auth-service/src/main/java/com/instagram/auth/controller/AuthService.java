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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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
     * GET /api/v1/auth/profiles/{userId} → { userId, username, firstName, lastName, bio, profileImageUrl, isPrivate }.
     */
    public ProfileResponse getProfileByUserId(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("User not found"));
        Profile profile = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));
        return ProfileResponse.builder()
                .userId(user.getId())
                .username(profile.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(profile.getBio())
                .profileImageUrl(profile.getProfileImageUrl())
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

    /**
     * Ažurira profil korisnika (ime, prezime, username, bio, profilna slika, privatnost).
     * PUT /api/v1/auth/profiles/{userId}
     */
    public ProfileResponse updateProfile(Integer userId, String firstName, String lastName, 
                                         String username, String bio, Boolean isPrivate, 
                                         MultipartFile profileImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("User not found"));
        Profile profile = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));

        // Provera da li username vec postoji kod drugog korisnika
        if (!profile.getUsername().equals(username) &&
                profileRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException("Username već postoji");
        }

        // Update User entity
        user.setFirstName(firstName);
        user.setLastName(lastName);
        userRepository.save(user);

        // Update Profile entity
        profile.setUsername(username);
        profile.setBio(bio);
        
        // Obrada profile slike ako je dostupna
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String base64Image = "data:" + profileImage.getContentType() + ";base64," + 
                        Base64.getEncoder().encodeToString(profileImage.getBytes());
                profile.setProfileImageUrl(base64Image);
            } catch (IOException e) {
                throw new RuntimeException("Failed to process profile image", e);
            }
        }
        
        if (isPrivate != null) {
            profile.setIsPrivate(isPrivate);
        }
        profileRepository.save(profile);

        return ProfileResponse.builder()
                .userId(user.getId())
                .username(profile.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(profile.getBio())
                .profileImageUrl(profile.getProfileImageUrl())
                .isPrivate(profile.getIsPrivate())
                .build();
    }
}