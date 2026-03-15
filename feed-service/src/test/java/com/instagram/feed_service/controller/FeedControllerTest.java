package com.instagram.feed_service.controller;

import com.instagram.feed_service.dto.FeedResponseDTO;
import com.instagram.feed_service.service.FeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedController.class)
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedService feedService;

    @Test
    void getFeed_returnsOk() throws Exception {
        when(feedService.getUserFeed(anyLong(), nullable(String.class)))
                .thenReturn(List.of(FeedResponseDTO.builder().postId(1L).build()));

        mockMvc.perform(get("/api/feed/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value(1));
    }
}
