package com.instagram.interaction_service.service;

import com.instagram.interaction_service.dto.RelationshipStatusResponse;
import com.instagram.interaction_service.entity.Comment;
import com.instagram.interaction_service.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CommentService commentService;

    @Test
    void addComment_saves() {
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment comment = commentService.addComment(10L, 7L, "hello");

        assertEquals(10L, comment.getPostId());
        assertEquals(7L, comment.getUserId());
        assertEquals("hello", comment.getContent());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void getCommentsByPost_filtersBlocked() {
        List<Comment> comments = List.of(
                Comment.builder().id(1L).postId(5L).userId(1L).content("a").build(),
                Comment.builder().id(2L).postId(5L).userId(2L).content("b").build()
        );
        when(commentRepository.findByPostId(5L)).thenReturn(comments);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(RelationshipStatusResponse.class)
        )).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            boolean blocked = url.endsWith("/relationship/2");
            return ResponseEntity.ok(new RelationshipStatusResponse(null, null, blocked));
        });

        List<Comment> result = commentService.getCommentsByPost(5L, "Bearer token");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
    }

    @Test
    void getCommentsByPost_returnsAllWithoutAuth() {
        List<Comment> comments = List.of(
                Comment.builder().id(1L).postId(5L).userId(1L).content("a").build(),
                Comment.builder().id(2L).postId(5L).userId(2L).content("b").build()
        );
        when(commentRepository.findByPostId(5L)).thenReturn(comments);

        List<Comment> result = commentService.getCommentsByPost(5L, null);

        assertEquals(2, result.size());
    }

    @Test
    void updateComment_updatesWhenOwner() {
        Comment comment = Comment.builder().id(1L).userId(5L).content("hi").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment updated = commentService.updateComment(1L, 5L, "new content");

        assertEquals("new content", updated.getContent());
        verify(commentRepository).save(comment);
    }

    @Test
    void deleteComment_throwsWhenNotOwner() {
        Comment comment = Comment.builder().id(1L).userId(5L).content("hi").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> commentService.deleteComment(1L, 7L));

        assertTrue(ex.getMessage().toLowerCase().contains("not authorized"));
    }
}
