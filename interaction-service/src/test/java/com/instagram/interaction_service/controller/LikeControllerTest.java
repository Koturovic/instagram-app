package com.instagram.interaction_service.controller;

import com.instagram.interaction_service.service.LikeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LikeController.class)
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LikeService likeService;

    @Test
    void toggleLike_returnsOk() throws Exception {
        when(likeService.toggleLike(5L, 7L)).thenReturn("Post liked");

        mockMvc.perform(post("/api/likes/5").param("userId", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string("Post liked"));
    }

    @Test
    void getLikesCount_returnsOk() throws Exception {
        when(likeService.getLikesCount(anyLong(), nullable(String.class))).thenReturn(3L);

        mockMvc.perform(get("/api/likes/5/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }
}
