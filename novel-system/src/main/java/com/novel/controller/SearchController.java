package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.model.Novel;
import com.novel.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 关键词搜索
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Novel>>> search(@RequestParam String keyword) {
        List<Novel> results = searchService.searchNovels(keyword);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * 高级搜索（带筛选条件）
     */
    @GetMapping("/advanced")
    public ResponseEntity<ApiResponse<Map<String, Object>>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer minWordCount,
            @RequestParam(required = false) Integer maxWordCount,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Novel> results = searchService.advancedSearch(
                keyword, category, author, minWordCount, maxWordCount, 
                status, sortBy, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", results.getContent());
        response.put("totalElements", results.getTotalElements());
        response.put("totalPages", results.getTotalPages());
        response.put("currentPage", results.getNumber());
        response.put("size", results.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 按分类搜索
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<Novel>>> searchByCategory(
            @PathVariable String category) {
        List<Novel> results = searchService.searchByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * 按作者搜索
     */
    @GetMapping("/author")
    public ResponseEntity<ApiResponse<List<Novel>>> searchByAuthor(
            @RequestParam String author) {
        List<Novel> results = searchService.searchByAuthor(author);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * 获取搜索建议
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        List<String> suggestions = searchService.getSearchSuggestions(keyword, limit);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    /**
     * 获取热门搜索关键词
     */
    @GetMapping("/hot-keywords")
    public ResponseEntity<ApiResponse<List<String>>> getHotKeywords() {
        List<String> keywords = searchService.getHotSearchKeywords();
        return ResponseEntity.ok(ApiResponse.success(keywords));
    }
}
