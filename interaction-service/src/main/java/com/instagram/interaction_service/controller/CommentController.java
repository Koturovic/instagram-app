package com.instagram.interaction_service.controller;

import com.instagram.interaction_service.entity.Comment;
import com.instagram.interaction_service.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{postId}")
    public Comment addComment(
            @PathVariable Long postId,
            @RequestParam Long userId,
            @RequestBody String content) {
        return commentService.addComment(postId, userId, content);
    }

    @GetMapping("/{postId}")
    public List<Comment> getComments(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return commentService.getCommentsByPost(postId, authHeader);
    }

    @GetMapping("/{postId}/count")
    public Long getCommentsCount(@PathVariable Long postId) {
        return commentService.getCommentsCount(postId);
    }

    @PutMapping("/{commentId}")
    public Comment updateComment(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @RequestBody String content) {
        return commentService.updateComment(commentId, userId, content);
    }

    @DeleteMapping("/{commentId}")
    public String deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentService.deleteComment(commentId, userId);
        return "Komentar je obrisan.";
    }
}
