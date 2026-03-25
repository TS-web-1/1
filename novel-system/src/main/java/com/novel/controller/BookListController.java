package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.model.BookList;
import com.novel.model.User;
import com.novel.repository.UserRepository;
import com.novel.service.BookListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/booklists")
public class BookListController {

    private final BookListService bookListService;
    private final UserRepository userRepository;

    @Autowired
    public BookListController(BookListService bookListService, UserRepository userRepository) {
        this.bookListService = bookListService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ApiResponse<BookList> createBookList(@RequestBody BookListRequest request) {
        BookList bookList = new BookList();
        bookList.setUserId(request.getUserId());
        bookList.setTitle(request.getTitle());
        bookList.setDescription(request.getDescription());
        String listType = request.isPublic() ? "PUBLIC" : "PRIVATE";
        bookList.setListType(listType);
        
        BookList savedBookList = bookListService.createBookList(bookList, request.getNovelIds());
        
        return ApiResponse.success(savedBookList);
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<BookList>> getUserBookLists(@PathVariable Long userId) {
        List<BookList> bookLists = bookListService.getUserBookLists(userId);
        return ApiResponse.success(bookLists);
    }

    @GetMapping("/public")
    public ApiResponse<List<BookList>> getPublicBookLists() {
        List<BookList> bookLists = bookListService.getPublicBookLists();
        return ApiResponse.success(bookLists);
    }

    @GetMapping("/public/paginated")
    public ApiResponse<Map<String, Object>> getPublicBookListsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<BookList> bookListPage = bookListService.getPublicBookListsPaginated(page, size);
        
        // 手动构建响应数据，包含用户名
        List<Map<String, Object>> contentWithUser = new ArrayList<>();
        for (BookList bookList : bookListPage.getContent()) {
            Map<String, Object> bookListMap = new HashMap<>();
            bookListMap.put("id", bookList.getId());
            bookListMap.put("userId", bookList.getUserId());
            bookListMap.put("title", bookList.getTitle());
            bookListMap.put("description", bookList.getDescription());
            bookListMap.put("listType", bookList.getListType());
            bookListMap.put("bookCount", bookList.getBookCount());
            bookListMap.put("likeCount", bookList.getLikeCount());
            bookListMap.put("createdAt", bookList.getCreatedAt());
            
            // 获取用户名
            User user = userRepository.findById(bookList.getUserId()).orElse(null);
            if (user != null) {
                bookListMap.put("creatorName", user.getUsername());
            } else {
                bookListMap.put("creatorName", "未知用户");
            }
            
            contentWithUser.add(bookListMap);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", contentWithUser);
        response.put("totalElements", bookListPage.getTotalElements());
        response.put("totalPages", bookListPage.getTotalPages());
        response.put("currentPage", bookListPage.getNumber());
        response.put("size", bookListPage.getSize());
        response.put("numberOfElements", bookListPage.getNumberOfElements());
        
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getBookListDetail(@PathVariable Long id) {
        Map<String, Object> bookListDetail = bookListService.getBookListDetail(id);
        return ApiResponse.success(bookListDetail);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBookList(@PathVariable Long id) {
        bookListService.deleteBookList(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/admin/all")
    public ApiResponse<Map<String, Object>> getAllBookListsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BookList> bookListPage = bookListService.getAllBookListsPaginated(page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", bookListPage.getContent());
        response.put("totalElements", bookListPage.getTotalElements());
        response.put("totalPages", bookListPage.getTotalPages());
        response.put("currentPage", bookListPage.getNumber());
        response.put("size", bookListPage.getSize());
        
        return ApiResponse.success(response);
    }

    @DeleteMapping("/admin/{id}")
    public ApiResponse<Void> deleteBookListForAdmin(@PathVariable Long id) {
        bookListService.deleteBookList(id);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/admin/make-all-public")
    public ApiResponse<String> makeAllBookListsPublic() {
        int count = bookListService.makeAllBookListsPublic();
        return ApiResponse.success("已将 " + count + " 个书单设置为公开", null);
    }

    // 请求体 DTO
    public static class BookListRequest {
        private Long userId;
        private String title;
        private String description;
        @com.fasterxml.jackson.annotation.JsonProperty("isPublic")
        private boolean isPublic;
        private List<Long> novelIds;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("isPublic")
        public boolean isPublic() {
            return isPublic;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("isPublic")
        public void setPublic(boolean aPublic) {
            isPublic = aPublic;
        }

        public List<Long> getNovelIds() {
            return novelIds;
        }

        public void setNovelIds(List<Long> novelIds) {
            this.novelIds = novelIds;
        }
    }
}
