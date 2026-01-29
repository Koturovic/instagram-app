package com.instagram.auth.auth;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vq/auth")
@RequiredArgsConstructor
public class AuthController {


        private AuthService service;
        @PostMapping("/register")
        public ResponseEntity<AuthenticationResponse> register(
                @RequestBody RegisterRequest request
        ){
            return ResponseEntity.ok(service.register(request));
        }

}
