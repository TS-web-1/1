package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.dto.NovelRequest;
import com.novel.model.Chapter;
import com.novel.model.Novel;
import com.novel.model.User;
import com.novel.service.NovelService;
import com.novel.service.ReadingService;
import com.novel.service.UserBehaviorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/novels")
public class NovelController {

    private final NovelService novelService;
    private final ReadingService readingService;
    private final JdbcTemplate jdbcTemplate;
    private final UserBehaviorService userBehaviorService;

    @Autowired
    public NovelController(NovelService novelService, ReadingService readingService,
                          JdbcTemplate jdbcTemplate, UserBehaviorService userBehaviorService) {
        this.novelService = novelService;
        this.readingService = readingService;
        this.jdbcTemplate = jdbcTemplate;
        this.userBehaviorService = userBehaviorService;
    }

    @PostMapping
    public ApiResponse<Novel> createNovel(@Valid @RequestBody NovelRequest request) {
        Novel novel = novelService.createNovel(request);
        return ApiResponse.success(novel);
    }

    @GetMapping("/{id}")
    public ApiResponse<Novel> getNovelById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        Novel novel = novelService.getNovelById(id);
        novelService.incrementNovelViews(id);

        if (user != null) {
            userBehaviorService.recordView(user.getId(), id);
        }

        return ApiResponse.success(novel);
    }

    @GetMapping
    public ApiResponse<Page<Novel>> getAllNovels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        Page<Novel> novels = novelService.getAllNovels(page, size, category, status);
        return ApiResponse.success(novels);
    }

    @GetMapping("/category/{category}")
    public ApiResponse<List<Novel>> getNovelsByCategory(@PathVariable String category) {
        List<Novel> novels = novelService.getNovelsByCategory(category);
        return ApiResponse.success(novels);
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<Novel>> getNovelsByStatus(@PathVariable String status) {
        List<Novel> novels = novelService.getNovelsByStatus(status);
        return ApiResponse.success(novels);
    }

    @GetMapping("/search")
    public ApiResponse<List<Novel>> searchNovels(@RequestParam String keyword) {
        List<Novel> novels = novelService.searchNovels(keyword);
        return ApiResponse.success(novels);
    }

    @PutMapping("/{id}")
    public ApiResponse<Novel> updateNovel(@PathVariable Long id, @Valid @RequestBody NovelRequest request) {
        Novel novel = novelService.updateNovel(id, request);
        return ApiResponse.success(novel);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNovel(@PathVariable Long id) {
        novelService.deleteNovel(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/{novelId}/chapters")
    public ResponseEntity<ApiResponse<List<Chapter>>> getNovelChapters(@PathVariable Long novelId) {
        List<Chapter> chapters = readingService.getNovelChapters(novelId);
        return ResponseEntity.ok(ApiResponse.success(chapters));
    }

    @GetMapping("/{novelId}/chapters/{chapterId}")
    public ResponseEntity<ApiResponse<Chapter>> getChapter(@PathVariable Long novelId, @PathVariable Long chapterId) {
        Chapter chapter = readingService.getChapter(chapterId);
        if (chapter != null) {
            return ResponseEntity.ok(ApiResponse.success(chapter));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("章节不存在"));
    }

    @PostMapping("/init-stats")
    public ResponseEntity<ApiResponse<String>> initAllNovelStats() {
        novelService.initAllNovelStats();
        return ResponseEntity.ok(ApiResponse.success("统计数据初始化完成"));
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<byte[]> getNovelCover(@PathVariable Long id) {
        try {
            String sql = "SELECT cover_image FROM novels WHERE id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, id);

            if (results.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String coverUrl = (String) results.get(0).get("cover_image");

            if (coverUrl == null || coverUrl.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            URL url = new URL(coverUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                return ResponseEntity.notFound().build();
            }

            String contentType = conn.getContentType();
            if (contentType == null) {
                contentType = "image/jpeg";
            }

            byte[] imageBytes = conn.getInputStream().readAllBytes();
            conn.disconnect();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(imageBytes.length);
            headers.setCacheControl("max-age=3600");

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{novelId}/like")
    public ResponseEntity<ApiResponse<String>> likeNovel(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId) {
        try {
            boolean success = novelService.likeNovel(user.getId(), novelId);
            if (success) {
                userBehaviorService.recordLike(user.getId(), novelId);
                return ResponseEntity.ok(ApiResponse.success("点赞成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.success("您已点赞过"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("点赞失败：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{novelId}/like")
    public ResponseEntity<ApiResponse<String>> unlikeNovel(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId) {
        try {
            boolean success = novelService.unlikeNovel(user.getId(), novelId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("取消点赞成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.success("您还未点赞"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("取消点赞失败：" + e.getMessage()));
        }
    }

    @GetMapping("/{novelId}/like/check")
    public ResponseEntity<ApiResponse<Boolean>> checkLike(
            @AuthenticationPrincipal User user,
            @PathVariable Long novelId) {
        try {
            boolean hasLiked = novelService.hasLiked(user.getId(), novelId);
            return ResponseEntity.ok(ApiResponse.success(hasLiked));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("检查失败：" + e.getMessage()));
        }
    }
}
