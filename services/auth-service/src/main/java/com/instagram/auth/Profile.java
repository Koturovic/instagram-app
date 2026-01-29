package com.instagram.auth;

import jakarta.persistence.*;
import lombok.*;

@Entity   // jedan Profile (objekat) = jedan red u bazi; 1 User ima 1 Profile (veza 1:1)
@Table(name = "profiles", indexes = {
	@Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", unique = true, nullable = false)
	private User user;

	@Column(name = "user_name",nullable = false,unique = true)
	private String username;

	@Column(name = "is_private", nullable = false)
	@Builder.Default
	private Boolean isPrivate = false;



}
