package com.instagram.interaction_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "likes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId; // ID posta iz post-servisa
    private Long userId; // ID korisnika koji je lajkovao
    private LocalDateTime createdAt;
}