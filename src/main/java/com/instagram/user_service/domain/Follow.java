package com.instagram.user_service.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Prihvaćena relacija praćenja. Za zahteve za praćenje (privatni profili) koristi se FollowRequest.
 */
@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_user_id", "following_user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_user_id", nullable = false)
    private Long followerUserId;

    @Column(name = "following_user_id", nullable = false)
    private Long followingUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
