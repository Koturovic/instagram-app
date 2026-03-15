package com.instagram.post_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_media")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PostMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileUrl; // Putanja do fajla na disku/S3
    private String contentType; // npr. "image/jpeg" ili "video/mp4"

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post; // Veza ka objavi kojoj fajl pripada
}