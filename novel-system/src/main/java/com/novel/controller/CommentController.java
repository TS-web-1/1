package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.model.Comment;
import com.novel.model.User;
import com.novel.service.CommentService;
import com.novel.service.UserBehaviorService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comments")
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentService commentService;
    private final UserBehaviorService userBehaviorService;

    public CommentController(CommentService commentService, UserBehaviorService userBehaviorService) {
        this.commentService = commentService;
        this.userBehaviorService = userBehaviorService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Comment>> addComment(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long novelId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam String content,
            @RequestParam(defaultValue = "NOVEL") String commentType,
            @RequestParam(required = false) Long parentId) {

        if (novelId == null && chapterId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("必须指定小说ID或章节ID"));
        }

        Comment comment = commentService.addComment(user.getId(), novelId, chapterId,
                content, commentType, parentId);

        if (novelId != null) {
            userBehaviorService.recordComment(user.getId(), novelId);
        }

        return ResponseEntity.ok(ApiResponse.success(comment));
    }

    @GetMapping("/novel/{novelId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNovelComments(
            @PathVariable Long novelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        Page<Comment> comments = commentService.getNovelComments(novelId, page, size, sortBy);

        Map<String, Object> result = new HashMap<>();
        result.put("content", comments.getContent());
        result.put("totalElements", comments.getTotalElements());
        result.put("totalPages", comments.getTotalPages());
        result.put("currentPage", comments.getNumber());
        result.put("size", comments.getSize());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/novel/{novelId}/popular")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPopularComments(
            @PathVariable Long novelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Comment> comments = commentService.getPopularComments(novelId, page, size);

        Map<String, Object> result = new HashMap<>();
        result.put("content", comments.getContent());
        result.put("totalElements", comments.getTotalElements());
        result.put("totalPages", comments.getTotalPages());
        result.put("currentPage", comments.getNumber());
        result.put("size", comments.getSize());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChapterComments(
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Comment> comments = commentService.getChapterComments(chapterId, page, size);

        Map<String, Object> result = new HashMap<>();
        result.put("content", comments.getContent());
        result.put("totalElements", comments.getTotalElements());
        result.put("totalPages", comments.getTotalPages());
        result.put("currentPage", comments.getNumber());
        result.put("size", comments.getSize());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Comment>>> getUserComments(
            @PathVariable Long userId) {

        List<Comment> comments = commentService.getUserComments(userId);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<List<Comment>>> getCommentReplies(
            @PathVariable Long commentId) {

        List<Comment> replies = commentService.getCommentReplies(commentId);
        return ResponseEntity.ok(ApiResponse.success(replies));
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<String>> likeComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId) {

        boolean success = commentService.likeComment(user.getId(), commentId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("点赞成功"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("已经点赞过了"));
        }
    }

    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<String>> unlikeComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId) {

        boolean success = commentService.unlikeComment(user.getId(), commentId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("取消点赞成功"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("还没有点赞过"));
        }
    }

    @GetMapping("/{commentId}/like/check")
    public ResponseEntity<ApiResponse<Boolean>> checkLike(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId) {

        boolean hasLiked = commentService.hasLiked(user.getId(), commentId);
        return ResponseEntity.ok(ApiResponse.success(hasLiked));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<String>> deleteComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId) {

        boolean success = commentService.deleteComment(commentId, user.getId());
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("评论删除成功"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无权删除该评论或评论不存在"));
        }
    }

    @GetMapping("/stats/{novelId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCommentStats(
            @PathVariable Long novelId) {

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalComments", commentService.countNovelComments(novelId));

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Comment>>> getAllComments() {
        List<Comment> comments = commentService.getAllComments();
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
}
