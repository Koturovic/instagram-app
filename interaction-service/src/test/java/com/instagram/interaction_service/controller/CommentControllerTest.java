package com.instagram.interaction_service.controller;

import com.instagram.interaction_service.entity.Comment;
import com.instagram.interaction_service.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @Test
    void getComments_returnsOk() throws Exception {
        when(commentService.getCommentsByPost(anyLong(), nullable(String.class)))
                .thenReturn(List.of(Comment.builder().id(1L).content("hi").build()));

        mockMvc.perform(get("/api/comments/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void addComment_returnsOk() throws Exception {
        when(commentService.addComment(9L, 3L, "hello"))
                .thenReturn(Comment.builder().id(5L).content("hello").build());

        mockMvc.perform(post("/api/comments/9")
                        .param("userId", "3")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }
}
