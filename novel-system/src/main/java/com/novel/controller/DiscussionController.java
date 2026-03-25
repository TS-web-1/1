package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.dto.DiscussionRequest;
import com.novel.model.Discussion;
import com.novel.model.User;
import com.novel.service.DiscussionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/discussions")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @PostMapping
    public ApiResponse<Discussion> createTopic(@Valid @RequestBody DiscussionRequest request, @AuthenticationPrincipal User user) {
        Discussion topic = discussionService.createTopic(request, user);
        return ApiResponse.success(topic);
    }

    @GetMapping
    public ApiResponse<List<Discussion>> getAllTopics() {
        List<Discussion> topics = discussionService.getAllTopics();
        return ApiResponse.success(topics);
    }

    @GetMapping("/paginated")
    public ApiResponse<Map<String, Object>> getTopicsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Discussion> topicPage = discussionService.getTopicsPaginated(page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", topicPage.getContent());
        response.put("totalElements", topicPage.getTotalElements());
        response.put("totalPages", topicPage.getTotalPages());
        response.put("currentPage", topicPage.getNumber());
        response.put("size", topicPage.getSize());
        
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<Discussion> getTopicById(@PathVariable Long id) {
        Discussion topic = discussionService.getTopicById(id);
        return ApiResponse.success(topic);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTopic(@PathVariable Long id, @AuthenticationPrincipal User user) {
        discussionService.deleteTopic(id, user);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/admin/all")
    public ApiResponse<Map<String, Object>> getAllTopicsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Discussion> topicPage = discussionService.getTopicsPaginated(page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", topicPage.getContent());
        response.put("totalElements", topicPage.getTotalElements());
        response.put("totalPages", topicPage.getTotalPages());
        response.put("currentPage", topicPage.getNumber());
        response.put("size", topicPage.getSize());
        
        return ApiResponse.success(response);
    }

    @DeleteMapping("/admin/{id}")
    public ApiResponse<Void> deleteTopicForAdmin(@PathVariable Long id) {
        discussionService.deleteTopicById(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Discussion>> getTopicsByUser(@PathVariable Long userId) {
        User dummyUser = new User();
        dummyUser.setId(userId);
        List<Discussion> topics = discussionService.getTopicsByUser(dummyUser);
        return ApiResponse.success(topics);
    }
}
