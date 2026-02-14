package com.instagram.auth.config;

import com.instagram.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserDetailsAdapter")
class UserDetailsAdapterTest {

    private UserDetailsAdapter adapter;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1)
                .email("aaa@test.com")
                .firstName("Ana")
                .lastName("Ivanovic")
                .password("ana1234567")
                .createdAt(Instant.now())
                .build();
        adapter = new UserDetailsAdapter(user);
    }

    @Test
    @DisplayName("getUsername vraća email korisnika")
    void getUsername_returnsEmail() {
        assertThat(adapter.getUsername()).isEqualTo("aaa@test.com");
    }

    @Test
    @DisplayName("getPassword vraća lozinku korisnika")
    void getPassword_returnsPassword() {
        assertThat(adapter.getPassword()).isEqualTo("ana1234567");
    }

    @Test
    @DisplayName("getAuthorities sadrži ROLE_USER")
    void getAuthorities_containsRoleUser() {
        assertThat(adapter.getAuthorities())
                .anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
    }

    @Test
    @DisplayName("isAccountNonExpired vraća true")
    void isAccountNonExpired_returnsTrue() {
        assertThat(adapter.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("isAccountNonLocked vraća true")
    void isAccountNonLocked_returnsTrue() {
        assertThat(adapter.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("isCredentialsNonExpired vraća true")
    void isCredentialsNonExpired_returnsTrue() {
        assertThat(adapter.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("isEnabled vraća true")
    void isEnabled_returnsTrue() {
        assertThat(adapter.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("getUser vraća isti User objekat")
    void getUser_returnsUser() {
        assertThat(adapter.getUser()).isNotNull();
        assertThat(adapter.getUser().getEmail()).isEqualTo("aaa@test.com");
    }
}