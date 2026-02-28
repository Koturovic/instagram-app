package com.instagram.user_service.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private Profile follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private Profile following;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum FollowStatus {
        PENDING,   // za privatne profile – čeka prihvatanje
        ACCEPTED   // javni profil ili prihvaćen zahtev
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
