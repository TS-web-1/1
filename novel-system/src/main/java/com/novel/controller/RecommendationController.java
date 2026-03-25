package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.model.Novel;
import com.novel.service.RecommendationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.novel.model.User;
import java.util.List;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * 获取个性化推荐
     */
    @GetMapping("/personalized")
    public ApiResponse<List<Novel>> getPersonalizedRecommendations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "100") int limit) {
        
        if (user == null) {
            // 用户未登录时，返回热门推荐
            List<Novel> recommendations = recommendationService.getPopularRecommendations(limit);
            return ApiResponse.success("获取推荐成功", recommendations);
        }
        
        List<Novel> recommendations = recommendationService.getPersonalizedRecommendations(user.getId(), limit);
        return ApiResponse.success("获取个性化推荐成功", recommendations);
    }

    /**
     * 获取热门推荐（未登录用户也可以查看）
     * 基于全局热门度：阅读量 × 0.6 + 收藏量 × 0.4
     */
    @GetMapping("/popular")
    public ApiResponse<List<Novel>> getPopularRecommendations(
            @RequestParam(defaultValue = "100") int limit) {
        List<Novel> recommendations = recommendationService.getPopularRecommendations(limit);
        return ApiResponse.success("获取热门推荐成功", recommendations);
    }
    
    /**
     * 获取重磅推荐（按收藏数排序）
     */
    @GetMapping("/bookmarks")
    public ApiResponse<List<Novel>> getBookmarkRecommendations(
            @RequestParam(defaultValue = "100") int limit) {
        List<Novel> recommendations = recommendationService.getBookmarkRecommendations(limit);
        return ApiResponse.success("获取重磅推荐成功", recommendations);
    }
    
    /**
     * 获取热门推荐（按阅读历史排序）
     */
    @GetMapping("/reading-history")
    public ApiResponse<List<Novel>> getReadingHistoryRecommendations(
            @RequestParam(defaultValue = "100") int limit) {
        List<Novel> recommendations = recommendationService.getReadingHistoryRecommendations(limit);
        return ApiResponse.success("获取热门推荐成功", recommendations);
    }
    
    /**
     * 获取完结精品推荐
     */
    @GetMapping("/completed")
    public ApiResponse<List<Novel>> getCompletedRecommendations(
            @RequestParam(defaultValue = "100") int limit) {
        List<Novel> recommendations = recommendationService.getCompletedRecommendations(limit);
        return ApiResponse.success("获取完结精品推荐成功", recommendations);
    }
}
