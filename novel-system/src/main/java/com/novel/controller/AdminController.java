package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.model.Category;
import com.novel.dto.ChapterDTO;
import com.novel.model.Chapter;
import com.novel.model.Novel;
import com.novel.model.User;
import com.novel.service.AdminService;
import com.novel.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AdminController(AdminService adminService, JwtUtil jwtUtil) {
        this.adminService = adminService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        System.out.println("AdminController - test() called");
        return ResponseEntity.ok(ApiResponse.success("Test successful"));
    }

    @PostMapping("/chapters/approve-all")
    public ResponseEntity<ApiResponse<String>> approveAllChapters() {
        int count = adminService.approveAllChapters();
        return ResponseEntity.ok(ApiResponse.success("已将 " + count + " 个章节设置为通过状态"));
    }

    @PostMapping("/novels/batch-update-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchUpdateNovelStatus() {
        int wordCountThreshold = 3000000;

        long count = adminService.countOngoingNovelsWithWordCount(wordCountThreshold);
        int updatedCount = adminService.updateOngoingToCompleted(wordCountThreshold);

        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("totalCount", count);
        result.put("wordCountThreshold", wordCountThreshold);

        return ResponseEntity.ok(ApiResponse.success(
            "已将 " + updatedCount + " 本字数超过 300 万的连载中小说改为完结状态", result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adminLogin(
            @RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        User admin = adminService.validateAdmin(username, password);
        if (admin != null) {
            String token = jwtUtil.generateToken(admin.getUsername());
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("username", admin.getUsername());
            result.put("role", admin.getRole());
            return ResponseEntity.ok(ApiResponse.success("管理员登录成功", result));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("管理员账号或密码错误"));
    }

    @GetMapping("/novels/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingNovels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Novel> novels = adminService.getPendingNovelsPaginated(page, size);
        Map<String, Object> result = Map.of(
            "content", novels.getContent(),
            "totalElements", novels.getTotalElements(),
            "totalPages", novels.getTotalPages(),
            "currentPage", novels.getNumber(),
            "size", novels.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/novels")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllNovels(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Novel> novels = adminService.getAllNovelsPaginated(keyword, status, page, size);
        Map<String, Object> result = Map.of(
            "content", novels.getContent(),
            "totalElements", novels.getTotalElements(),
            "totalPages", novels.getTotalPages(),
            "currentPage", novels.getNumber(),
            "size", novels.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/novels/{novelId}/review")
    public ResponseEntity<ApiResponse<String>> reviewNovel(
            @PathVariable Long novelId,
            @RequestParam String status,
            @RequestParam(required = false) String reviewComment) {
        boolean success = adminService.reviewNovel(novelId, status, reviewComment);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("审核完成"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("审核失败"));
    }

    @PostMapping("/novels/{novelId}/ban")
    public ResponseEntity<ApiResponse<String>> banNovel(@PathVariable Long novelId) {
        boolean success = adminService.banNovel(novelId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("小说已封禁"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("小说不存在"));
    }

    @PostMapping("/novels/{novelId}/unban")
    public ResponseEntity<ApiResponse<String>> unbanNovel(@PathVariable Long novelId) {
        boolean success = adminService.unbanNovel(novelId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("小说已解封"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("小说不存在"));
    }

    @DeleteMapping("/novels/{novelId}")
    public ResponseEntity<ApiResponse<String>> deleteNovel(@PathVariable Long novelId) {
        boolean success = adminService.deleteNovel(novelId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("小说已删除"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("小说不存在"));
    }

    @PutMapping("/novels/{novelId}/status")
    public ResponseEntity<ApiResponse<String>> updateNovelStatus(
            @PathVariable Long novelId,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        String reviewStatus = request.get("reviewStatus");
        boolean success = adminService.updateNovelStatus(novelId, status, reviewStatus);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("状态更新成功"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("小说不存在"));
    }

    @GetMapping("/chapters/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingChapters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        System.out.println("AdminController - getPendingChapters called with page=" + page + ", size=" + size);
        Page<ChapterDTO> chapters = adminService.getPendingChaptersPaginated(page, size);
        System.out.println("AdminController - getPendingChapters found " + chapters.getTotalElements() + " chapters");

        java.util.List<Map<String, Object>> content = new java.util.ArrayList<>();
        for (ChapterDTO chapter : chapters.getContent()) {
            Map<String, Object> chapterMap = new java.util.HashMap<>();
            chapterMap.put("id", chapter.getId());
            chapterMap.put("novelId", chapter.getNovelId());
            chapterMap.put("title", chapter.getTitle());
            chapterMap.put("chapterNumber", chapter.getChapterNumber());
            chapterMap.put("wordCount", chapter.getWordCount());
            chapterMap.put("reviewStatus", chapter.getReviewStatus());
            chapterMap.put("createdAt", chapter.getCreatedAt());
            content.add(chapterMap);
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("content", content);
        result.put("totalElements", chapters.getTotalElements());
        result.put("totalPages", chapters.getTotalPages());
        result.put("currentPage", chapters.getNumber());
        result.put("size", chapters.getSize());

        System.out.println("AdminController - Result map created: " + result);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(result);
        System.out.println("AdminController - ApiResponse created, returning");

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        ResponseEntity<ApiResponse<Map<String, Object>>> responseEntity = ResponseEntity.ok()
            .headers(headers)
            .body(response);
        System.out.println("AdminController - ResponseEntity created with headers, about to return");
        return responseEntity;
    }

    @GetMapping("/chapters")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllChapters(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Chapter> chapters = adminService.getAllChaptersPaginated(keyword, status, page, size);
        Map<String, Object> result = Map.of(
            "content", chapters.getContent(),
            "totalElements", chapters.getTotalElements(),
            "totalPages", chapters.getTotalPages(),
            "currentPage", chapters.getNumber(),
            "size", chapters.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/chapters/{chapterId}")
    public ResponseEntity<ApiResponse<Chapter>> getChapterDetail(@PathVariable Long chapterId) {
        Chapter chapter = adminService.getChapterById(chapterId);
        if (chapter != null) {
            return ResponseEntity.ok(ApiResponse.success(chapter));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("章节不存在"));
    }

    @PostMapping("/chapters/{chapterId}/review")
    public ResponseEntity<ApiResponse<String>> reviewChapter(
            @AuthenticationPrincipal User user,
            @PathVariable Long chapterId,
            @RequestParam String status,
            @RequestParam(required = false) String reviewComment) {
        boolean success = adminService.reviewChapter(chapterId, status, reviewComment, user.getId());
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("审核完成"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("审核失败"));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<Category>> addCategory(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String icon,
            @RequestBody(required = false) Map<String, String> body) {
        if (name == null && body != null) {
            name = body.get("name");
            description = body.get("description");
            icon = body.get("icon");
        }
        Category category = adminService.addCategory(name, description, icon);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<String>> updateCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String icon,
            @RequestBody(required = false) Map<String, String> body) {
        if (name == null && body != null) {
            name = body.get("name");
            description = body.get("description");
            icon = body.get("icon");
        }
        boolean success = adminService.updateCategory(categoryId, name, description, icon);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("分类更新成功"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("分类不存在"));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long categoryId) {
        boolean success = adminService.deleteCategory(categoryId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("分类删除成功"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("分类不存在"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        List<Category> categories = adminService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/categories/paginated")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategoriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Category> categories = adminService.getAllCategories(page, size);
        Map<String, Object> result = Map.of(
            "content", categories.getContent(),
            "totalElements", categories.getTotalElements(),
            "totalPages", categories.getTotalPages(),
            "currentPage", categories.getNumber()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/categories/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategoryStats() {
        Map<String, Object> stats = adminService.getCategoryStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> users = adminService.getAllUsers(page, size);
        Map<String, Object> result = Map.of(
            "content", users.getContent(),
            "totalElements", users.getTotalElements(),
            "totalPages", users.getTotalPages(),
            "currentPage", users.getNumber()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<List<User>>> searchUsers(@RequestParam String keyword) {
        List<User> users = adminService.searchUsers(keyword);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<ApiResponse<String>> banUser(@PathVariable Long userId) {
        boolean success = adminService.banUser(userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("用户已封禁"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
    }

    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<ApiResponse<String>> unbanUser(@PathVariable Long userId) {
        boolean success = adminService.unbanUser(userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("用户已解封"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long userId) {
        boolean success = adminService.deleteUser(userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("用户已删除"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在或无法删除"));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String email = request.get("email");
        String role = request.get("role");

        User user = adminService.createUser(username, password, email, role);
        if (user != null) {
            return ResponseEntity.ok(ApiResponse.success(user));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("创建失败，用户名已存在或角色无效"));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlatformStats() {
        Map<String, Object> stats = adminService.getPlatformStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/stats/reading-behavior")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReadingBehaviorReport() {
        Map<String, Object> report = adminService.getReadingBehaviorReport();
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/stats/trending-genres")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTrendingGenres() {
        List<Map<String, Object>> trends = adminService.getTrendingGenres();
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Map<String, Object>> comments = adminService.getAllCommentsPaginated(page, size);
        Map<String, Object> result = Map.of(
            "content", comments.getContent(),
            "totalElements", comments.getTotalElements(),
            "totalPages", comments.getTotalPages(),
            "currentPage", comments.getNumber(),
            "size", comments.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<String>> deleteComment(@PathVariable Long commentId) {
        boolean success = adminService.deleteComment(commentId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("评论已删除"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("评论不存在"));
    }

    @DeleteMapping("/novels/all")
    public ResponseEntity<ApiResponse<String>> deleteAllNovels() {
        try {
            adminService.deleteAllNovels();
            return ResponseEntity.ok(ApiResponse.success("所有小说数据已删除"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }
}
