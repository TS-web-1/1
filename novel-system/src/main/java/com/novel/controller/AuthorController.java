package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.model.*;
import com.novel.service.AuthorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/author")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    // ========== 作品管理 ==========

    @GetMapping("/novels")
    public ResponseEntity<ApiResponse<List<Novel>>> getAuthorNovels(
            @AuthenticationPrincipal User user) {
        try {
            List<Novel> novels = authorService.getAuthorNovels(user.getId());
            return ResponseEntity.ok(ApiResponse.success(novels));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/novels")
    public ResponseEntity<ApiResponse<Novel>> createNovel(
            @AuthenticationPrincipal User user,
            @RequestBody Novel novel) {
        try {
            Novel created = authorService.createNovel(novel, user.getId());
            return ResponseEntity.ok(ApiResponse.success(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/novels/{novelId}")
    public ResponseEntity<ApiResponse<Novel>> updateNovel(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId,
            @RequestBody Novel novel) {
        try {
            Novel updated = authorService.updateNovel(novelId, novel, user.getId());
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/novels/{novelId}")
    public ResponseEntity<ApiResponse<Void>> deleteNovel(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId) {
        try {
            authorService.deleteNovel(novelId, user.getId());
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== 章节管理 ==========

    @GetMapping("/novels/{novelId}/chapters")
    public ResponseEntity<ApiResponse<List<Chapter>>> getChapters(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId) {
        try {
            List<Chapter> chapters = authorService.getNovelChapters(novelId, user.getId());
            return ResponseEntity.ok(ApiResponse.success(chapters));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/novels/{novelId}/chapters")
    public ResponseEntity<ApiResponse<Chapter>> createChapter(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId,
            @RequestBody Chapter chapter) {
        try {
            Chapter created = authorService.createChapter(novelId, chapter, user.getId());
            return ResponseEntity.ok(ApiResponse.success(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/chapters/{chapterId}")
    public ResponseEntity<ApiResponse<Chapter>> updateChapter(
            @AuthenticationPrincipal User user,
            @PathVariable Long chapterId,
            @RequestBody Chapter chapter) {
        try {
            Chapter updated = authorService.updateChapter(chapterId, chapter, user.getId());
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/chapters/{chapterId}")
    public ResponseEntity<ApiResponse<Void>> deleteChapter(
            @AuthenticationPrincipal User user,
            @PathVariable Long chapterId) {
        try {
            authorService.deleteChapter(chapterId, user.getId());
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== 草稿箱 ==========

    @GetMapping("/drafts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDrafts(
            @AuthenticationPrincipal User user) {
        try {
            List<Map<String, Object>> drafts = authorService.getDrafts(user.getId());
            return ResponseEntity.ok(ApiResponse.success(drafts));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/drafts")
    public ResponseEntity<ApiResponse<DraftChapter>> saveDraft(
            @AuthenticationPrincipal User user,
            @RequestBody DraftChapter draft) {
        try {
            DraftChapter saved = authorService.saveDraft(draft, user.getId());
            return ResponseEntity.ok(ApiResponse.success(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/drafts/{draftId}")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(
            @AuthenticationPrincipal User user,
            @PathVariable Long draftId) {
        try {
            authorService.deleteDraft(draftId, user.getId());
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== 评论管理 ==========

    @GetMapping("/novels/{novelId}/comments")
    public ResponseEntity<ApiResponse<List<Comment>>> getComments(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId) {
        try {
            List<Comment> comments = authorService.getNovelComments(novelId, user.getId());
            return ResponseEntity.ok(ApiResponse.success(comments));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId) {
        try {
            authorService.deleteComment(commentId, user.getId());
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== 数据统计 ==========

    @GetMapping("/novels/{novelId}/stats")
    public ResponseEntity<ApiResponse<NovelStats>> getNovelStats(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId) {
        try {
            NovelStats stats = authorService.getNovelStats(novelId, user.getId());
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthorStats(
            @AuthenticationPrincipal User user) {
        try {
            NovelStats stats = authorService.getAuthorTotalStats(user.getId());
            Long novelCount = authorService.countNovelsByAuthorId(user.getId());

            Map<String, Object> statsMap = new java.util.HashMap<>();
            statsMap.put("totalNovels", novelCount != null ? novelCount : 0);
            statsMap.put("totalViews", stats != null && stats.getTotalViews() != null ? stats.getTotalViews() : 0);
            statsMap.put("totalBookmarks", stats != null && stats.getTotalBookmarks() != null ? stats.getTotalBookmarks() : 0);
            statsMap.put("totalLikes", stats != null && stats.getTotalLikes() != null ? stats.getTotalLikes() : 0);
            statsMap.put("totalComments", stats != null && stats.getTotalComments() != null ? stats.getTotalComments() : 0);
            statsMap.put("totalWords", stats != null && stats.getTotalWords() != null ? stats.getTotalWords() : 0);
            statsMap.put("totalChapters", stats != null && stats.getTotalChapters() != null ? stats.getTotalChapters() : 0);

            return ResponseEntity.ok(ApiResponse.success(statsMap));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/reading-trend")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReadingTrend(
            @AuthenticationPrincipal User user) {
        try {
            List<Map<String, Object>> trend = authorService.getReadingTrend(user.getId());
            return ResponseEntity.ok(ApiResponse.success("获取阅读趋势成功", trend));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
