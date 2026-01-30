package com.instagram.post_service.controller;

import com.instagram.post_service.entity.Post;
import com.instagram.post_service.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // Kreiranje posta
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Post createPost(
            @RequestParam("description") String description,
            @RequestParam("userId") Long userId,
            @RequestParam("files") List<MultipartFile> files) throws Exception {
        return postService.createPostWithMedia(description, userId, files);
    }

    // Listanje svih postova
    @GetMapping
    public List<Post> getAll() {
        return postService.getAllPosts();
    }

    // LISTANJE POSTOVA PO USER ID-u (Ovde je bila greška - sada je @GetMapping)
    @GetMapping("/user/{userId}")
    public List<Post> getPostsByUserId(@PathVariable Long userId) {
        return postService.getPostsByUserId(userId);
    }

    // Brisanje posta
    @DeleteMapping("/{id}")
    public String deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return "Post sa ID-jem " + id + " je uspešno obrisan.";
    }

    // Ažuriranje posta
    @PutMapping("/{id}")
    public Post updatePost(
            @PathVariable Long id,
            @RequestParam("description") String description,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) throws Exception {
        return postService.updatePost(id, description, files);
    }
}