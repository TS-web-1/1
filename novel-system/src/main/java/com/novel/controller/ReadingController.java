package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.model.Bookmark;
import com.novel.model.Chapter;
import com.novel.model.ReadingProgress;
import com.novel.model.User;
import com.novel.service.ReadingService;
import com.novel.service.UserBehaviorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reading")
@CrossOrigin(origins = "*")
public class ReadingController {

    private final ReadingService readingService;
    private final UserBehaviorService userBehaviorService;

    public ReadingController(ReadingService readingService, UserBehaviorService userBehaviorService) {
        this.readingService = readingService;
        this.userBehaviorService = userBehaviorService;
    }

    @PostMapping("/progress/{novelId}")
    public ResponseEntity<ApiResponse<ReadingProgress>> saveProgress(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId,
            @RequestParam Long chapterId,
            @RequestParam Integer position,
            @RequestParam Double progressPercent,
            @RequestParam Integer readTime) {

        try {
            ReadingProgress progress = readingService.saveReadingProgress(
                    user.getId(), novelId, chapterId, position, progressPercent, readTime);

            if (readTime != null && readTime > 0) {
                userBehaviorService.recordReadTime(user.getId(), novelId, chapterId, readTime);
            }

            return ResponseEntity.ok(ApiResponse.success(progress));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("保存阅读进度失败: " + e.getMessage()));
        }
    }

    @GetMapping("/progress/{novelId}")
    public ResponseEntity<ApiResponse<ReadingProgress>> getProgress(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId) {

        try {
            ReadingProgress progress = readingService.getReadingProgress(user.getId(), novelId);
            return ResponseEntity.ok(ApiResponse.success(progress));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取阅读进度失败: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ReadingProgress>>> getReadingHistory(
            @AuthenticationPrincipal User user) {

        try {
            List<ReadingProgress> history = readingService.getUserReadingHistory(user.getId());
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取阅读历史失败: " + e.getMessage()));
        }
    }

    @PostMapping("/bookmark")
    public ResponseEntity<ApiResponse<Bookmark>> addBookmark(
            @AuthenticationPrincipal User user,
            @RequestParam Long novelId,
            @RequestParam Long chapterId,
            @RequestParam String title,
            @RequestParam Integer position,
            @RequestParam(required = false) String contentPreview) {

        try {
            Bookmark bookmark = readingService.addBookmark(
                    user.getId(), novelId, chapterId, title, position, contentPreview);
            return ResponseEntity.ok(ApiResponse.success(bookmark));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("添加书签失败: " + e.getMessage()));
        }
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<ApiResponse<List<Bookmark>>> getBookmarks(
            @AuthenticationPrincipal User user) {

        try {
            List<Bookmark> bookmarks = readingService.getUserBookmarks(user.getId());
            return ResponseEntity.ok(ApiResponse.success(bookmarks));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取书签失败: " + e.getMessage()));
        }
    }

    @GetMapping("/bookmarks/{novelId}")
    public ResponseEntity<ApiResponse<List<Bookmark>>> getNovelBookmarks(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId) {

        try {
            List<Bookmark> bookmarks = readingService.getNovelBookmarks(user.getId(), novelId);
            return ResponseEntity.ok(ApiResponse.success(bookmarks));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取书签失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/bookmark/{bookmarkId}")
    public ResponseEntity<ApiResponse<String>> deleteBookmark(
            @AuthenticationPrincipal User user,
            @PathVariable Long bookmarkId) {

        try {
            readingService.deleteBookmarkById(bookmarkId, user.getId());
            return ResponseEntity.ok(ApiResponse.success("书签删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("删除书签失败: " + e.getMessage()));
        }
    }

    @GetMapping("/bookmark/check")
    public ResponseEntity<ApiResponse<Boolean>> checkBookmark(
            @AuthenticationPrincipal User user,
            @RequestParam Long novelId,
            @RequestParam Long chapterId) {

        try {
            boolean hasBookmark = readingService.hasBookmark(user.getId(), novelId, chapterId);
            return ResponseEntity.ok(ApiResponse.success(hasBookmark));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("检查书签失败: " + e.getMessage()));
        }
    }

    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<Chapter>> getChapter(@PathVariable Long chapterId) {
        Chapter chapter = readingService.getChapter(chapterId);
        if (chapter != null) {
            return ResponseEntity.ok(ApiResponse.success(chapter));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("章节不存在"));
    }

    @GetMapping("/novels/{novelId}/chapters")
    public ResponseEntity<ApiResponse<List<Chapter>>> getNovelChapters(@PathVariable Long novelId) {
        List<Chapter> chapters = readingService.getNovelChapters(novelId);
        return ResponseEntity.ok(ApiResponse.success(chapters));
    }

    @GetMapping("/chapters/{novelId}")
    public ResponseEntity<ApiResponse<List<Chapter>>> getNovelChaptersCompat(@PathVariable Long novelId) {
        List<Chapter> chapters = readingService.getNovelChapters(novelId);
        return ResponseEntity.ok(ApiResponse.success(chapters));
    }

    @GetMapping("/chapter/navigation/{novelId}")
    public ResponseEntity<ApiResponse<Map<String, Chapter>>> getChapterNavigation(
            @PathVariable Long novelId,
            @RequestParam Long currentChapterId) {
        try {
            Chapter previous = readingService.getPreviousChapter(novelId, currentChapterId);
            Chapter next = readingService.getNextChapter(novelId, currentChapterId);

            Map<String, Chapter> navigation = new HashMap<>();
            navigation.put("previous", previous);
            navigation.put("next", next);

            return ResponseEntity.ok(ApiResponse.success(navigation));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取章节导航失败：" + e.getMessage()));
        }
    }
}
