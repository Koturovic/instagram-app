package com.instagram.auth.repository;

import com.instagram.auth.Profile;
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
@DisplayName("ProfileRepository")
class ProfileRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private User savedUser;
    private Profile savedProfile;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        userRepository.deleteAll();

        savedUser = userRepository.save(User.builder()
                .email("ana@test.com")
                .firstName("Ana")
                .lastName("Ivanovic")
                .password("encoded123")
                .build());

        savedProfile = profileRepository.save(Profile.builder()
                .user(savedUser)
                .username("anaivan")
                .isPrivate(false)
                .build());
    }

    @Nested
    @DisplayName("findByUser")
    class FindByUser {

        @Test
        @DisplayName("vraća Profile kada User postoji")
        void whenUserExists_returnsProfile() {
            Optional<Profile> result = profileRepository.findByUser(savedUser);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(savedProfile.getId());
            assertThat(result.get().getUsername()).isEqualTo("anaivan");
            assertThat(result.get().getUser().getId()).isEqualTo(savedUser.getId());
        }

        @Test
        @DisplayName("vraća empty kada User nema profile")
        void whenUserHasNoProfile_returnsEmpty() {
            User userWithoutProfile = userRepository.save(User.builder()
                    .email("drugi@test.com")
                    .firstName("Marko")
                    .lastName("Markovic")
                    .password("pass")
                    .build());

            Optional<Profile> result = profileRepository.findByUser(userWithoutProfile);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUser_Id")
    class FindByUserId {

        @Test
        @DisplayName("vraća Profile kada userId postoji")
        void whenUserIdExists_returnsProfile() {
            Optional<Profile> result = profileRepository.findByUser_Id(savedUser.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("anaivan");
        }

        @Test
        @DisplayName("vraća empty kada userId ne postoji")
        void whenUserIdNotExists_returnsEmpty() {
            Optional<Profile> result = profileRepository.findByUser_Id(99999);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByUsername")
    class ExistsByUsername {

        @Test
        @DisplayName("vraća true kada username postoji")
        void whenUsernameExists_returnsTrue() {
            boolean exists = profileRepository.existsByUsername("anaivan");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("vraća false kada username ne postoji")
        void whenUsernameNotExists_returnsFalse() {
            boolean exists = profileRepository.existsByUsername("nepostojeci");

            assertThat(exists).isFalse();
        }
    }
}
