package com.novel.service;

import com.novel.dto.NovelRequest;
import com.novel.model.Novel;
import com.novel.model.NovelLike;
import com.novel.model.NovelStats;
import com.novel.repository.NovelRepository;
import com.novel.repository.NovelStatsRepository;
import com.novel.repository.NovelLikeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NovelService {

    private final NovelRepository novelRepository;
    private final NovelStatsRepository novelStatsRepository;
    private final NovelLikeRepository novelLikeRepository;

    public NovelService(NovelRepository novelRepository, NovelStatsRepository novelStatsRepository, NovelLikeRepository novelLikeRepository) {
        this.novelRepository = novelRepository;
        this.novelStatsRepository = novelStatsRepository;
        this.novelLikeRepository = novelLikeRepository;
    }

    public Novel createNovel(NovelRequest request) {
        Novel novel = new Novel();
        novel.setTitle(request.getTitle());
        novel.setAuthor(request.getAuthor());
        novel.setDescription(request.getDescription());
        novel.setCategory(request.getCategory());
        novel.setCoverImage(request.getCoverImage());
        novel.setContent(request.getContent());
        novel.setStatus(request.getStatus() != null ? request.getStatus() : "ONGOING");
        novel.setReviewStatus("PENDING");

        Novel savedNovel = novelRepository.save(novel);
        
        // 创建小说统计数据
        NovelStats stats = new NovelStats();
        stats.setNovelId(savedNovel.getId());
        stats.setTotalViews(0L);
        stats.setTotalLikes(0L);
        stats.setTotalComments(0L);
        stats.setTotalBookmarks(0L);
        stats.setTotalWords(0L);
        stats.setTotalChapters(0);
        novelStatsRepository.save(stats);
        
        return savedNovel;
    }

    public Novel updateNovel(Long id, NovelRequest request) {
        Novel novel = novelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("小说不存在"));

        if (request.getTitle() != null) novel.setTitle(request.getTitle());
        if (request.getAuthor() != null) novel.setAuthor(request.getAuthor());
        if (request.getDescription() != null) novel.setDescription(request.getDescription());
        if (request.getCategory() != null) novel.setCategory(request.getCategory());
        if (request.getCoverImage() != null) novel.setCoverImage(request.getCoverImage());
        if (request.getContent() != null) novel.setContent(request.getContent());
        if (request.getStatus() != null) novel.setStatus(request.getStatus());

        novel.setUpdatedAt(new Date());
        return novelRepository.save(novel);
    }

    public Novel getNovelById(Long id) {
        return novelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("小说不存在"));
    }

    // 返回所有小说（支持分页）
    public Page<Novel> getAllNovels(int page, int size, String category, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        List<Novel> allNovels = novelRepository.findAll();
        
        // 过滤已审核通过的小说（允许 null、APPROVED 或空字符串）
        List<Novel> filteredNovels = allNovels.stream()
                .filter(n -> n.getReviewStatus() == null || 
                            "APPROVED".equals(n.getReviewStatus()) || 
                            n.getReviewStatus().isEmpty())
                .collect(Collectors.toList());
        
        // 按分类过滤
        if (category != null && !category.isEmpty()) {
            filteredNovels = filteredNovels.stream()
                    .filter(n -> category.equals(n.getCategory()))
                    .collect(Collectors.toList());
        }
        
        // 按状态过滤
        if (status != null && !status.isEmpty()) {
            filteredNovels = filteredNovels.stream()
                    .filter(n -> status.equals(n.getStatus()))
                    .collect(Collectors.toList());
        }
        
        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredNovels.size());
        
        if (start >= filteredNovels.size()) {
            return new org.springframework.data.domain.PageImpl<>(
                    List.of(), pageable, filteredNovels.size());
        }
        
        return new org.springframework.data.domain.PageImpl<>(
                filteredNovels.subList(start, end), pageable, filteredNovels.size());
    }

    // 返回所有小说（不分页，保持向后兼容）
    public List<Novel> getAllNovels() {
        return novelRepository.findAll();
    }

    // 返回指定分类的所有小说
    public List<Novel> getNovelsByCategory(String category) {
        System.out.println("获取分类小说: " + category);
        // 获取所有小说
        List<Novel> allNovels = novelRepository.findAll();
        System.out.println("所有小说数量: " + allNovels.size());
        
        // 检查分类分布
        Map<String, Long> categoryCount = allNovels.stream()
                .collect(Collectors.groupingBy(Novel::getCategory, Collectors.counting()));
        System.out.println("分类分布: " + categoryCount);
        
        // 检查审核状态
        Map<String, Long> statusCount = allNovels.stream()
                .collect(Collectors.groupingBy(Novel::getReviewStatus, Collectors.counting()));
        System.out.println("审核状态分布: " + statusCount);
        
        // 过滤出指定分类的小说
        List<Novel> filteredNovels = allNovels.stream()
                .filter(n -> {
                    System.out.println("小说分类: " + n.getCategory() + " vs 请求分类: " + category + ", 审核状态: " + n.getReviewStatus());
                    return category.equals(n.getCategory());
                })
                .toList();
        
        System.out.println("过滤后小说数量: " + filteredNovels.size());
        return filteredNovels;
    }

    // 返回指定状态的所有小说
    public List<Novel> getNovelsByStatus(String status) {
        return novelRepository.findAll()
                .stream()
                .filter(n -> status.equals(n.getStatus()))
                .toList();
    }

    // 搜索所有小说
    public List<Novel> searchNovels(String keyword) {
        return novelRepository.findByTitleContaining(keyword);
    }

    public void deleteNovel(Long id) {
        novelRepository.deleteById(id);
    }
    
    // 增加小说阅读量
    public void incrementNovelViews(Long novelId) {
        novelRepository.findById(novelId).ifPresent(novel -> {
            novel.setViews(novel.getViews() + 1);
            novelRepository.save(novel);
        });
        
        // 同步更新 NovelStats 表的阅读量
        novelStatsRepository.findByNovelId(novelId).ifPresent(stats -> {
            stats.setTotalViews(stats.getTotalViews() + 1L);
            stats.setDailyViews(stats.getDailyViews() + 1L);
            stats.setWeeklyViews(stats.getWeeklyViews() + 1L);
            stats.setMonthlyViews(stats.getMonthlyViews() + 1L);
            novelStatsRepository.save(stats);
        });
    }
    
    // 初始化所有小说的统计数据
    public void initAllNovelStats() {
        List<Novel> novels = novelRepository.findAll();
        for (Novel novel : novels) {
            if (novelStatsRepository.findByNovelId(novel.getId()).isEmpty()) {
                NovelStats stats = new NovelStats();
                stats.setNovelId(novel.getId());
                stats.setTotalViews(novel.getViews() != null ? novel.getViews() : 0L);
                stats.setTotalLikes(0L);
                stats.setTotalComments(0L);
                stats.setTotalBookmarks(novel.getBookmarks() != null ? novel.getBookmarks() : 0L);
                stats.setTotalWords(0L);
                stats.setTotalChapters(0);
                novelStatsRepository.save(stats);
            }
        }
    }
    
    /**
     * 点赞小说
     */
    public boolean likeNovel(Long userId, Long novelId) {
        if (novelLikeRepository.existsByUserIdAndNovelId(userId, novelId)) {
            return false;
        }
        
        NovelLike novelLike = new NovelLike();
        novelLike.setUserId(userId);
        novelLike.setNovelId(novelId);
        novelLikeRepository.save(novelLike);
        
        novelRepository.findById(novelId).ifPresent(novel -> {
            novel.setTotalLikes(novel.getTotalLikes() + 1);
            novelRepository.save(novel);
        });
        
        novelStatsRepository.findByNovelId(novelId).ifPresent(stats -> {
            stats.setTotalLikes(stats.getTotalLikes() + 1);
            novelStatsRepository.save(stats);
        });
        
        return true;
    }
    
    /**
     * 取消点赞小说
     */
    public boolean unlikeNovel(Long userId, Long novelId) {
        if (!novelLikeRepository.existsByUserIdAndNovelId(userId, novelId)) {
            return false;
        }
        
        novelLikeRepository.findByUserIdAndNovelId(userId, novelId)
            .ifPresent(novelLike -> novelLikeRepository.delete(novelLike));
        
        novelRepository.findById(novelId).ifPresent(novel -> {
            if (novel.getTotalLikes() > 0) {
                novel.setTotalLikes(novel.getTotalLikes() - 1);
                novelRepository.save(novel);
            }
        });
        
        novelStatsRepository.findByNovelId(novelId).ifPresent(stats -> {
            if (stats.getTotalLikes() > 0) {
                stats.setTotalLikes(stats.getTotalLikes() - 1);
                novelStatsRepository.save(stats);
            }
        });
        
        return true;
    }
    
    /**
     * 检查用户是否已点赞
     */
    public boolean hasLiked(Long userId, Long novelId) {
        return novelLikeRepository.existsByUserIdAndNovelId(userId, novelId);
    }
}
