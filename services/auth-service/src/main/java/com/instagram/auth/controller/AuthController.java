package com.instagram.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {
        // 1. prvo proveravamo da li postji Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);

        // 2.pozovemo servis da validira token
        try {
            ValidateResponse response = service.validateToken(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Dohvata profil po userId – za user-service (follow/block logika).
     * 200 → { userId, username, isPrivate }; 404 ako user/profile ne postoji.
     */
    @GetMapping("/profiles/{userId}")
    public ResponseEntity<ProfileResponse> getProfileByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(service.getProfileByUserId(userId));
    }

    /**
     * Pretraga profila po username-u ili imenu/prezimenu (auth_db).
     * Koristi ga user-service za Search stranicu.
     */
    @GetMapping("/profiles/search")
    public ResponseEntity<List<ProfileSearchResponse>> searchProfiles(@RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(service.searchProfiles(q));
    }

    /**
     * Ažurira profil korisnika (ime, prezime, username, bio, privatnost + opciono slika).
     * PUT /api/v1/auth/profiles/{userId}
     * Content-Type: multipart/form-data
     */
    @PutMapping(value = "/profiles/{userId}", consumes = "multipart/form-data")
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable Integer userId,
            @RequestPart(value = "firstName") String firstName,
            @RequestPart(value = "lastName") String lastName,
            @RequestPart(value = "username") String username,
            @RequestPart(value = "bio", required = false) String bio,
            @RequestPart(value = "isPrivate", required = false) String isPrivateStr,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        
        Boolean isPrivate = isPrivateStr != null ? Boolean.parseBoolean(isPrivateStr) : null;
        return ResponseEntity.ok(
            service.updateProfile(userId, firstName, lastName, username, bio, isPrivate, profileImage)
        );
    }
}

// REsponse entity je klasa za odgovor na zahtev(omogucava da se vrati odgovor
// sa statusom i body-jem)

// @PostMapping("/register") - ovo je ruta za registraciju
// @Valid - validira REGISTERREQUEST pre nego sto metoda bude pozvana
// ako ne prodje vraca 400 BAD REQUEST i poruku o gresci

// TOK:
// Klijent šalje POST /api/v1/auth/register sa JSON-om.
// Validacija (npr. @NotBlank, @Email na RegisterRequest).
// Ako nema grešaka, poziva se service.register(request).
// Servis vraća AuthenticationResponse (sa tokenom).
// Kontroler vraća ResponseEntity.ok(...) → 200 OK i JSON sa tokenom.