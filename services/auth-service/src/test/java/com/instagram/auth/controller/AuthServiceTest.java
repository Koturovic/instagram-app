package com.instagram.auth.controller;

import com.instagram.auth.Profile;
import com.instagram.auth.User;
import com.instagram.auth.config.UserDetailsAdapter;
import com.instagram.auth.exception.EmailAlreadyExistsException;
import com.instagram.auth.exception.UsernameAlreadyExistsException;
import com.instagram.auth.repository.ProfileRepository;
import com.instagram.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


// Za svaki test unit Junit pokrene mockito -> kreira mockove i ubacuje ih u polja
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    // kreira laznu verziju interfejsa/klase
    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.instagram.auth.config.JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;



    // pomocne metode
    private static RegisterRequest validRegisterRequest(){

        return RegisterRequest.builder()
                .firstName("Petar")
                .lastName("Petrovic")
                .username("petarP")
                .email("petar@example.com")
                .password("lozinka123")
                .build();

    }

    private static LoginRequest validLoginRequest(){
        return LoginRequest.builder()
                .email("petar@example.com")
                .password("lozinka123")
                .build();
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("Vraca token kada su podaci validni")
        void success_returnToken(){
            var request = validRegisterRequest();
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(profileRepository.existsByUsername(request.getUsername())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(jwtService.generateToken(any())).thenReturn("jwt-token-123");

            var response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isNotBlank().isEqualTo("jwt-token-123");

            verify(userRepository).save(any(User.class));
            verify(profileRepository).save(any(Profile.class));
            verify(passwordEncoder).encode(request.getPassword());
        }

        @Test
        @DisplayName("Baca EmailAlreadyExistsException kada email vec postoji")

        void emailExists_throwsEmailAlreadyExists(){
            var request = validRegisterRequest();

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            assertThatThrownBy(()-> authService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessage("Email već postoji");

            verify(userRepository,never()).save(any());
            verify(profileRepository, never()).save(any());
        }


        @Test
        @DisplayName("baca UsernameAlreadyExistsException kada username već postoji")
        void usernameExists_throwsUsernameAlreadyExists() {
            var request = validRegisterRequest();
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(profileRepository.existsByUsername(request.getUsername())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessage("Username već postoji");

            verify(userRepository, never()).save(any());
            verify(profileRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("login")
    class Login{

        @Test
        @DisplayName("vraca token ukoliko su kredecncijali ispravni")
        void success_return_token(){
            var request = validLoginRequest();
            var user = User.builder()
                    .id(1)
                    .email(request.getEmail())
                    .password("encoded")
                    .firstName("Petar")
                    .lastName("Petrovic")
                    .build();


            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(jwtService.generateToken(any())).thenReturn("jwt-token-login");


            var response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isNotBlank().isEqualTo("jwt-token-login");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtService).generateToken(any(UserDetailsAdapter.class));
        }


        @Test
        @DisplayName("baca BadCredentialsException kada su kredencijali pogresni")
        void bad_credantial(){
            var request = validLoginRequest();
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("Bad credential"));
            assertThatThrownBy(()-> authService.login(request)).isInstanceOf(BadCredentialsException.class);

            verify(userRepository,never()).findByEmail(anyString());
            verify(jwtService, never()).generateToken(any());
        }


    }



}

