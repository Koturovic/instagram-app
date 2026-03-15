package com.instagram.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagram.user_service.dto.FollowRequestResponse;
import com.instagram.user_service.dto.UserSearchResultDto;
import com.instagram.user_service.exception.GlobalExceptionHandler;
import com.instagram.user_service.security.CurrentUser;
import com.instagram.user_service.service.FollowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController")
class UserControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FollowService followService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        UserController controller = new UserController(followService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/followers/count returns count")
    void getFollowersCount() throws Exception {
        when(followService.getFollowersCount(2L)).thenReturn(10L);
        mockMvc.perform(get("/api/v1/users/2/followers/count").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(10));
        verify(followService).getFollowersCount(2L);
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/following/count returns count")
    void getFollowingCount() throws Exception {
        when(followService.getFollowingCount(2L)).thenReturn(5L);
        mockMvc.perform(get("/api/v1/users/2/following/count").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
        verify(followService).getFollowingCount(2L);
    }

    @Test
    @DisplayName("POST /api/v1/users/follow-request/{targetUserId} returns 201 and body")
    void sendFollowRequest() throws Exception {
        FollowRequestResponse response = FollowRequestResponse.builder().requestId(100L).followed(false).build();
        when(followService.sendFollowRequest(1L, 2L)).thenReturn(response);
        setCurrentUser(1L);
        mockMvc.perform(post("/api/v1/users/follow-request/2").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(100))
                .andExpect(jsonPath("$.followed").value(false));
        verify(followService).sendFollowRequest(1L, 2L);
    }

    @Test
    @DisplayName("POST /api/v1/users/follow-request/{requestId}/accept returns 200")
    void acceptFollowRequest() throws Exception {
        setCurrentUser(1L);
        mockMvc.perform(post("/api/v1/users/follow-request/100/accept").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(followService).acceptFollowRequest(1L, 100L);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/following/{targetUserId} returns 204")
    void unfollow() throws Exception {
        setCurrentUser(1L);
        mockMvc.perform(delete("/api/v1/users/following/2"))
                .andExpect(status().isNoContent());
        verify(followService).unfollow(1L, 2L);
    }

    @Test
    @DisplayName("POST /api/v1/users/block/{targetUserId} returns 200")
    void block() throws Exception {
        setCurrentUser(1L);
        mockMvc.perform(post("/api/v1/users/block/2"))
                .andExpect(status().isOk());
        verify(followService).block(1L, 2L);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/block/{targetUserId} returns 204")
    void unblock() throws Exception {
        setCurrentUser(1L);
        mockMvc.perform(delete("/api/v1/users/block/2"))
                .andExpect(status().isNoContent());
        verify(followService).unblock(1L, 2L);
    }

    @Test
    @DisplayName("GET /api/v1/users/search returns search results")
    void search() throws Exception {
        when(followService.searchUsers("ana", 1L)).thenReturn(List.of(
                UserSearchResultDto.builder()
                        .id(2L)
                        .username("ana")
                        .firstName("Ana")
                        .lastName("Anić")
                        .isPrivate(false)
                        .build()
        ));

        setCurrentUser(1L);
        mockMvc.perform(get("/api/v1/users/search").param("q", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].username").value("ana"));

        verify(followService).searchUsers("ana", 1L);
    }

    private void setCurrentUser(long userId) {
        CurrentUser user = new CurrentUser(userId, "testuser");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Resolves @AuthenticationPrincipal CurrentUser in standalone MockMvc.
     */
    private static class CurrentUserArgumentResolver implements org.springframework.web.method.support.HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
            return parameter.getParameterType().equals(CurrentUser.class);
        }

        @Override
        public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                      org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                      org.springframework.web.context.request.NativeWebRequest webRequest,
                                      org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getPrincipal() : null;
        }
    }
}
