package com.instagram.post_service.service;

import com.instagram.post_service.entity.Post;
import com.instagram.post_service.entity.PostMedia;
import com.instagram.post_service.repository.PostRepository;
import io.minio.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MinioClient minioClient; // Automatski ubačen preko config klase
    private static final String BUCKET_NAME = "instagram-media";

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Post createPostWithMedia(String description, Long userId, List<MultipartFile> files) throws Exception {
        // 1. Validacija broja fajlova
        if (files.size() > 20) throw new RuntimeException("Maksimalno 20 fajlova!");

        // 2. Osiguraj da bucket postoji u MinIO
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
        }

        // 3. Kreiraj objekat posta
        Post post = Post.builder()
                .description(description)
                .userId(userId)
                .mediaFiles(new ArrayList<>())
                .build();

        // 4. Upload svakog fajla
        for (MultipartFile file : files) {
            // Validacija veličine (50MB)
            if (file.getSize() > 50 * 1024 * 1024) {
                throw new RuntimeException("Fajl " + file.getOriginalFilename() + " prelazi 50MB!");
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            // Slanje u MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Čuvanje URL-a (MinIO adresa) u bazu
            PostMedia media = PostMedia.builder()
                    .fileUrl("http://localhost:9000/" + BUCKET_NAME + "/" + fileName)
                    .contentType(file.getContentType())
                    .post(post)
                    .build();
            post.getMediaFiles().add(media);
        }

        return postRepository.save(post);
    }

    public List<Post> getPostsByUserId(Long userId) {
        return postRepository.findByUserId(userId);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post nije pronađen!"));

        for (PostMedia media : post.getMediaFiles()) {
            try {
                String fileName = media.getFileUrl().substring(media.getFileUrl().lastIndexOf("/") + 1);
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(BUCKET_NAME)
                                .object(fileName)
                                .build()
                );
            } catch (Exception e) {
                System.err.println("Greška pri brisanju sa MinIO: " + e.getMessage());
            }
        }
        postRepository.delete(post);
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post sa ID-jem " + id + " nije pronađen!"));
    }

    @Transactional
    public Post updatePost(Long postId, String description, List<MultipartFile> files) throws Exception {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post nije pronađen!"));

        // 1. Ažuriraj opis
        post.setDescription(description);

        // 2. Ako su poslate nove slike, zamenjujemo stare
        if (files != null && !files.isEmpty()) {
            // Prvo obriši stare slike sa MinIO-a
            for (PostMedia media : post.getMediaFiles()) {
                try {
                    String fileName = media.getFileUrl().substring(media.getFileUrl().lastIndexOf("/") + 1);
                    minioClient.removeObject(RemoveObjectArgs.builder().bucket(BUCKET_NAME).object(fileName).build());
                } catch (Exception e) {
                    System.err.println("Greška pri brisanju stare slike: " + e.getMessage());
                }
            }
            // Isprazni listu u bazi
            post.getMediaFiles().clear();

            // Dodaj nove slike
            for (MultipartFile file : files) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(BUCKET_NAME)
                                .object(fileName)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );

                PostMedia media = PostMedia.builder()
                        .fileUrl("http://localhost:9000/" + BUCKET_NAME + "/" + fileName)
                        .contentType(file.getContentType())
                        .post(post)
                        .build();
                post.getMediaFiles().add(media);
            }
        }
        return postRepository.save(post);
    }
}