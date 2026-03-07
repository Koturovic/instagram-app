package com.instagram.auth.repository;

import com.instagram.auth.Profile;
import com.instagram.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Integer> {

	Optional<Profile> findByUser(User user);

	Optional<Profile> findByUser_Id(Integer userId);

	boolean existsByUsername(String username);

	/**
	 * Pretraga profila po username-u ili po imenu/prezimenu korisnika (auth_db).
	 */
	@Query("SELECT p FROM Profile p JOIN FETCH p.user u WHERE LOWER(p.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))")
	List<Profile> searchByUsernameOrName(@Param("q") String q);
}
