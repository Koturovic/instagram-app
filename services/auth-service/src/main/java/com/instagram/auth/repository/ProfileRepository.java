package com.instagram.auth.repository;

import com.instagram.auth.Profile;
import com.instagram.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Integer> {

	Optional<Profile> findByUser(User user);

	Optional<Profile> findByUser_Id(Integer userId);

	boolean existsByUsername(String username);
}
