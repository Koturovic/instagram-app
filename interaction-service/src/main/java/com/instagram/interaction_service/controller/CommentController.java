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
    public List<Comment> getComments(@PathVariable Long postId) {
        return commentService.getCommentsByPost(postId);
    }

    @DeleteMapping("/{commentId}")
    public String deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return "Komentar je obrisan.";
    }
}