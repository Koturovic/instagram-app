package com.instagram.auth.repository;

import com.instagram.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        savedUser = userRepository.save(User.builder()
                .email("ana@test.com")
                .firstName("Ana")
                .lastName("Ivanovic")
                .password("encoded123")
                .build());
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("vraća User kada email postoji")
        void whenEmailExists_returnsUser() {
            Optional<User> result = userRepository.findByEmail("ana@test.com");

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(savedUser.getId());
            assertThat(result.get().getEmail()).isEqualTo("ana@test.com");
            assertThat(result.get().getFirstName()).isEqualTo("Ana");
        }

        @Test
        @DisplayName("vraća empty kada email ne postoji")
        void whenEmailNotExists_returnsEmpty() {
            Optional<User> result = userRepository.findByEmail("ne.postoji@test.com");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("vraća true kada email postoji")
        void whenEmailExists_returnsTrue() {
            boolean exists = userRepository.existsByEmail("ana@test.com");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("vraća false kada email ne postoji")
        void whenEmailNotExists_returnsFalse() {
            boolean exists = userRepository.existsByEmail("ne.postoji@test.com");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("čuva user sa @PrePersist createdAt")
        void save_setsCreatedAt() {
            assertThat(savedUser.getCreatedAt()).isNotNull();
        }
    }
}
