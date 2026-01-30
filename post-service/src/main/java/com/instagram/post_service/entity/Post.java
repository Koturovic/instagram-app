package com.instagram.post_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity // Ovo kaže Springu: "Napravi tabelu od ove klase"
@Table(name = "posts") // Tabela će se zvati 'posts'
@Getter @Setter // Lombok nam automatski pravi gettere i settere
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment ID (1, 2, 3...)
    private Long id;

    @Column(nullable = false)
    private Long userId; // Čuvamo samo ID korisnika koji je napravio objavu

    private String description; // Tekst objave

    @CreationTimestamp
    private LocalDateTime createdAt; // Datum kad je napravljeno

    // Inicijalizacija liste je bitna da ne dobiješ NullPointerException
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostMedia> mediaFiles = new ArrayList<>();
}