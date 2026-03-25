package com.novel.service;

import com.novel.model.Novel;
import com.novel.repository.NovelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    @Autowired
    private NovelRepository novelRepository;

    /**
     * 关键词搜索小说（只返回审核通过的）
     */
    public List<Novel> searchNovels(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String searchTerm = "%" + keyword.trim() + "%";
        List<Novel> novels = novelRepository.findByTitleContainingOrAuthorContainingOrDescriptionContaining(
                searchTerm, searchTerm, searchTerm);
        // 只返回审核通过的小说（包括未设置审核状态的老数据）
        return novels.stream()
                .filter(novel -> novel.getReviewStatus() == null || 
                        novel.getReviewStatus().isEmpty() || 
                        "APPROVED".equals(novel.getReviewStatus()))
                .toList();
    }

    /**
     * 高级搜索（带筛选条件）
     */
    public Page<Novel> advancedSearch(String keyword, String category, String author,
                                       Integer minWordCount, Integer maxWordCount,
                                       String status, String sortBy, int page, int size) {
        
        Pageable pageable = createPageable(sortBy, page, size);
        
        // 构建动态查询条件
        if ((keyword == null || keyword.isEmpty()) &&
            (category == null || category.isEmpty()) &&
            (author == null || author.isEmpty()) &&
            minWordCount == null && maxWordCount == null &&
            (status == null || status.isEmpty())) {
            // 只返回审核通过的小说（包括未设置审核状态的老数据）
            List<Novel> allNovels = novelRepository.findAll();
            List<Novel> approvedNovels = allNovels.stream()
                    .filter(novel -> novel.getReviewStatus() == null || 
                            novel.getReviewStatus().isEmpty() || 
                            "APPROVED".equals(novel.getReviewStatus()))
                    .toList();
            // 手动分页
            int start = page * size;
            int end = Math.min(start + size, approvedNovels.size());
            if (start >= approvedNovels.size()) {
                return Page.empty(pageable);
            }
            List<Novel> pageContent = approvedNovels.subList(start, end);
            return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, approvedNovels.size());
        }
        
        // 简化实现：先按关键词搜索，再过滤其他条件
        List<Novel> allNovels;
        if (keyword != null && !keyword.isEmpty()) {
            String searchTerm = "%" + keyword.trim() + "%";
            allNovels = novelRepository.findByTitleContainingOrAuthorContainingOrDescriptionContaining(
                    searchTerm, searchTerm, searchTerm);
        } else {
            allNovels = novelRepository.findAll();
        }
        
        // 应用筛选条件，只返回审核通过的小说（包括未设置审核状态的老数据）
        List<Novel> filteredNovels = allNovels.stream()
                .filter(novel -> novel.getReviewStatus() == null || 
                        novel.getReviewStatus().isEmpty() || 
                        "APPROVED".equals(novel.getReviewStatus()))
                .filter(novel -> category == null || category.isEmpty() ||
                        category.equals(novel.getCategory()))
                .filter(novel -> author == null || author.isEmpty() ||
                        novel.getAuthor().toLowerCase().contains(author.toLowerCase()))
                .filter(novel -> minWordCount == null ||
                        (novel.getWordCount() != null && novel.getWordCount() >= minWordCount))
                .filter(novel -> maxWordCount == null ||
                        (novel.getWordCount() != null && novel.getWordCount() <= maxWordCount))
                .filter(novel -> status == null || status.isEmpty() ||
                        status.equals(novel.getStatus()))
                .toList();
        
        // 手动分页
        int start = page * size;
        int end = Math.min(start + size, filteredNovels.size());
        
        if (start >= filteredNovels.size()) {
            return Page.empty(pageable);
        }
        
        List<Novel> pageContent = filteredNovels.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filteredNovels.size());
    }

    /**
     * 按分类搜索（只返回审核通过的）
     */
    public List<Novel> searchByCategory(String category) {
        List<Novel> novels = novelRepository.findByCategory(category);
        // 只返回审核通过的小说（包括未设置审核状态的老数据）
        return novels.stream()
                .filter(novel -> novel.getReviewStatus() == null || 
                        novel.getReviewStatus().isEmpty() || 
                        "APPROVED".equals(novel.getReviewStatus()))
                .toList();
    }

    /**
     * 按作者搜索（只返回审核通过的）
     */
    public List<Novel> searchByAuthor(String author) {
        if (author == null || author.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Novel> novels = novelRepository.findByAuthorContaining(author.trim());
        // 只返回审核通过的小说（包括未设置审核状态的老数据）
        return novels.stream()
                .filter(novel -> novel.getReviewStatus() == null || 
                        novel.getReviewStatus().isEmpty() || 
                        "APPROVED".equals(novel.getReviewStatus()))
                .toList();
    }

    /**
     * 按状态搜索
     */
    public List<Novel> searchByStatus(String status) {
        return novelRepository.findByStatus(status);
    }

    /**
     * 获取搜索建议（自动补全，只返回审核通过的）
     */
    public List<String> getSearchSuggestions(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String searchTerm = "%" + keyword.trim() + "%";
        List<Novel> novels = novelRepository.findByTitleContaining(searchTerm);

        // 只返回审核通过的小说标题（包括未设置审核状态的老数据）
        return novels.stream()
                .filter(novel -> novel.getReviewStatus() == null || 
                        novel.getReviewStatus().isEmpty() || 
                        "APPROVED".equals(novel.getReviewStatus()))
                .map(Novel::getTitle)
                .limit(limit)
                .toList();
    }

    /**
     * 获取热门搜索关键词
     */
    public List<String> getHotSearchKeywords() {
        // 简化实现，返回一些预设的热门关键词
        return List.of("玄幻", "都市", "修仙", "穿越", "重生", "系统", "无敌", "爽文");
    }

    private Pageable createPageable(String sortBy, int page, int size) {
        Sort sort;
        switch (sortBy) {
            case "views":
                sort = Sort.by(Sort.Direction.DESC, "views");
                break;
            case "bookmarks":
                sort = Sort.by(Sort.Direction.DESC, "bookmarks");
                break;
            case "rating":
                sort = Sort.by(Sort.Direction.DESC, "rating");
                break;
            case "wordCount":
                sort = Sort.by(Sort.Direction.DESC, "wordCount");
                break;
            case "createdAt":
            default:
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
        }
        return PageRequest.of(page, size, sort);
    }
}
