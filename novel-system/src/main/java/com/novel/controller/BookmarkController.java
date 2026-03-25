package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.dto.BookmarkRequest;
import com.novel.model.Bookmark;
import com.novel.model.User;
import com.novel.service.BookmarkService;
import com.novel.service.UserBehaviorService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookmarks")
@CrossOrigin(origins = "*")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final UserBehaviorService userBehaviorService;

    public BookmarkController(BookmarkService bookmarkService, UserBehaviorService userBehaviorService) {
        this.bookmarkService = bookmarkService;
        this.userBehaviorService = userBehaviorService;
    }

    @PostMapping
    public ApiResponse<Bookmark> createBookmark(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BookmarkRequest request) {
        try {
            Bookmark bookmark = bookmarkService.createBookmark(request, user);

            userBehaviorService.recordBookmark(user.getId(), request.getNovelId());

            return ApiResponse.success(bookmark);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<Bookmark>> getUserBookmarks(
            @AuthenticationPrincipal User user) {
        try {
            List<Bookmark> bookmarks = bookmarkService.getUserBookmarks(user.getId());
            return ApiResponse.success(bookmarks);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteBookmark(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        try {
            bookmarkService.deleteBookmark(id, user.getId());
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Bookmark> updateBookmark(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody BookmarkRequest request) {
        try {
            Bookmark bookmark = bookmarkService.updateBookmark(id, user.getId(), request);
            return ApiResponse.success(bookmark);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
