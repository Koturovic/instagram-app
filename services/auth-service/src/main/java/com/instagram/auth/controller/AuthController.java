package com.instagram.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
