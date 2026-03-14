package com.instagram.post_service.controller;

import com.instagram.post_service.entity.Post;
import com.instagram.post_service.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Test
    void getAll_returnsOk() throws Exception {
        when(postService.getAllPosts()).thenReturn(List.of());

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk());
    }

    @Test
    void createPost_returnsOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "photo.jpg",
                "image/jpeg",
                "data".getBytes()
        );

        when(postService.createPostWithMedia(eq("desc"), eq(5L), any(List.class)))
                .thenReturn(Post.builder().id(1L).description("desc").userId(5L).build());

        mockMvc.perform(multipart("/api/posts")
                        .file(file)
                        .param("description", "desc")
                        .param("userId", "5")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deletePost_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/posts/7"))
                .andExpect(status().isOk());
    }

    @Test
    void getPostsByUserId_returnsOk() throws Exception {
        when(postService.getPostsByUserId(5L)).thenReturn(List.of(Post.builder().id(1L).userId(5L).build()));

        mockMvc.perform(get("/api/posts/user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void deleteMediaFromPost_returnsOk() throws Exception {
        when(postService.deleteMediaFromPost(1L, 2L, 3L))
                .thenReturn(Post.builder().id(1L).build());

        mockMvc.perform(delete("/api/posts/1/media/2").param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updatePost_returnsOk() throws Exception {
        when(postService.updatePost(eq(1L), eq("updated desc"), any(List.class)))
                .thenReturn(Post.builder().id(1L).description("updated desc").build());

        mockMvc.perform(multipart("/api/posts/1")
                        .param("description", "updated desc")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk());
    }
}
