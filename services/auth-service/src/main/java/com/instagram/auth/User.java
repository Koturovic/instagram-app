package com.instagram.auth;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "users", indexes = {
		@Index(name = "idx_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "first_name", nullable = false, length = 255)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 255)
	private String lastName;

	@Column(nullable = false, length = 255)
	private String password;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
