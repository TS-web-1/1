package com.novel.service;

import com.novel.model.*;
import com.novel.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AuthorService {

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private DraftChapterRepository draftChapterRepository;

    @Autowired
    private NovelStatsRepository novelStatsRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // 用于调试端点
    public UserRepository getUserRepository() {
        return userRepository;
    }
    
    public NovelRepository getNovelRepository() {
        return novelRepository;
    }

    // ========== 作品管理 ==========
    
    public List<Novel> getAuthorNovels(Long authorId) {
        List<Novel> novels = novelRepository.findByAuthorId(authorId);
        // 为每个作品加载统计信息
        for (Novel novel : novels) {
            NovelStats stats = novelStatsRepository.findByNovelId(novel.getId()).orElse(null);
            if (stats != null) {
                novel.setViews(stats.getTotalViews() != null ? stats.getTotalViews().intValue() : 0);
                novel.setBookmarks(stats.getTotalBookmarks() != null ? stats.getTotalBookmarks().intValue() : 0);
                // 设置额外属性用于前端显示
                novel.setTotalChapters(stats.getTotalChapters());
                novel.setTotalWords(stats.getTotalWords() != null ? stats.getTotalWords().intValue() : 0);
                novel.setTotalLikes(stats.getTotalLikes() != null ? stats.getTotalLikes().intValue() : 0);
                novel.setTotalComments(stats.getTotalComments() != null ? stats.getTotalComments().intValue() : 0);
            }
        }
        return novels;
    }

    public Novel createNovel(Novel novel, Long authorId) {
        // 获取作者用户名
        User author = userRepository.findById(authorId).orElse(null);
        if (author != null) {
            novel.setAuthor(author.getUsername());
        }
        
        novel.setAuthorId(authorId);
        novel.setCreatedAt(new Date());
        novel.setUpdatedAt(new Date());
        novel.setViews(0);
        novel.setBookmarks(0);
        // 新创建的小说默认为待审核状态
        novel.setReviewStatus("PENDING");
        Novel saved = novelRepository.save(novel);
        
        // 创建统计记录
        NovelStats stats = new NovelStats();
        stats.setNovelId(saved.getId());
        novelStatsRepository.save(stats);
        
        return saved;
    }

    public Novel updateNovel(Long novelId, Novel novelData, Long authorId) {
        Optional<Novel> optional = novelRepository.findById(novelId);
        if (optional.isPresent()) {
            Novel novel = optional.get();
            if (!novel.getAuthorId().equals(authorId)) {
                throw new RuntimeException("无权修改此作品");
            }
            novel.setTitle(novelData.getTitle());
            novel.setDescription(novelData.getDescription());
            novel.setCoverImage(novelData.getCoverImage());
            novel.setCategory(novelData.getCategory());
            novel.setStatus(novelData.getStatus());
            novel.setUpdatedAt(new Date());
            return novelRepository.save(novel);
        }
        throw new RuntimeException("作品不存在");
    }

    public void deleteNovel(Long novelId, Long authorId) {
        Optional<Novel> optional = novelRepository.findById(novelId);
        if (optional.isPresent()) {
            Novel novel = optional.get();
            if (!novel.getAuthorId().equals(authorId)) {
                throw new RuntimeException("无权删除此作品");
            }
            // 删除相关数据
            chapterRepository.deleteByNovelId(novelId);
            draftChapterRepository.deleteAll(draftChapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId));
            // 通过 novelId 查找并删除统计记录
            novelStatsRepository.findByNovelId(novelId).ifPresent(stats -> 
                novelStatsRepository.delete(stats)
            );
            novelRepository.delete(novel);
        } else {
            throw new RuntimeException("作品不存在");
        }
    }

    // ========== 章节管理 ==========

    public List<Chapter> getNovelChapters(Long novelId, Long authorId) {
        // 验证作者权限
        Optional<Novel> novel = novelRepository.findById(novelId);
        if (novel.isPresent() && novel.get().getAuthorId().equals(authorId)) {
            return chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        }
        throw new RuntimeException("无权访问此作品");
    }

    public Chapter createChapter(Long novelId, Chapter chapter, Long authorId) {
        Optional<Novel> novel = novelRepository.findById(novelId);
        if (novel.isPresent() && novel.get().getAuthorId().equals(authorId)) {
            chapter.setNovelId(novelId);
            chapter.setCreatedAt(new Date());
            chapter.setUpdatedAt(new Date());
            // 确保新章节为待审核状态
            chapter.setReviewStatus("PENDING");
            
            // 设置章节序号
            List<Chapter> existingChapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
            int chapterNumber = existingChapters.size() + 1;
            chapter.setChapterNumber(chapterNumber);
            
            Chapter saved = chapterRepository.save(chapter);
            
            // 更新统计
            updateNovelStats(novelId);
            
            return saved;
        }
        throw new RuntimeException("无权操作此作品");
    }

    public Chapter updateChapter(Long chapterId, Chapter chapterData, Long authorId) {
        Optional<Chapter> optional = chapterRepository.findById(chapterId);
        if (optional.isPresent()) {
            Chapter chapter = optional.get();
            // 验证作者权限
            Optional<Novel> novel = novelRepository.findById(chapter.getNovelId());
            if (novel.isPresent() && novel.get().getAuthorId().equals(authorId)) {
                chapter.setTitle(chapterData.getTitle());
                chapter.setContent(chapterData.getContent());
                chapter.setWordCount(chapterData.getWordCount());
                chapter.setUpdatedAt(new Date());
                // 如果章节之前已通过审核、已被拒绝，或被退回修改（PENDING且有审核意见），更新后重新设置为待审核状态
                if ("APPROVED".equals(chapter.getReviewStatus()) || 
                    "REJECTED".equals(chapter.getReviewStatus()) ||
                    ("PENDING".equals(chapter.getReviewStatus()) && chapter.getReviewComment() != null)) {
                    chapter.setReviewStatus("PENDING");
                    chapter.setReviewComment(null);  // 清除审核意见，表示已重新提交
                    chapter.setReviewedAt(null);
                    chapter.setReviewedBy(null);
                }
                return chapterRepository.save(chapter);
            }
            throw new RuntimeException("无权修改此章节");
        }
        throw new RuntimeException("章节不存在");
    }

    public void deleteChapter(Long chapterId, Long authorId) {
        Optional<Chapter> optional = chapterRepository.findById(chapterId);
        if (optional.isPresent()) {
            Chapter chapter = optional.get();
            Optional<Novel> novel = novelRepository.findById(chapter.getNovelId());
            if (novel.isPresent() && novel.get().getAuthorId().equals(authorId)) {
                chapterRepository.delete(chapter);
                // 重新排序章节
                reorderChapters(chapter.getNovelId());
                // 更新统计
                updateNovelStats(chapter.getNovelId());
                return;
            }
            throw new RuntimeException("无权删除此章节");
        }
        throw new RuntimeException("章节不存在");
    }

    private void reorderChapters(Long novelId) {
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        for (int i = 0; i < chapters.size(); i++) {
            chapters.get(i).setChapterNumber(i + 1);
            chapterRepository.save(chapters.get(i));
        }
    }

    // ========== 草稿箱 ==========

    public List<Map<String, Object>> getDrafts(Long authorId) {
        List<DraftChapter> drafts = draftChapterRepository.findByAuthorIdOrderByUpdatedAtDesc(authorId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (DraftChapter draft : drafts) {
            Map<String, Object> draftInfo = new HashMap<>();
            draftInfo.put("id", draft.getId());
            draftInfo.put("novelId", draft.getNovelId());
            draftInfo.put("title", draft.getTitle());
            draftInfo.put("content", draft.getContent());
            draftInfo.put("chapterNumber", draft.getChapterNumber());
            draftInfo.put("wordCount", draft.getWordCount());
            draftInfo.put("createdAt", draft.getCreatedAt());
            draftInfo.put("updatedAt", draft.getUpdatedAt());
            
            // 获取小说标题
            Optional<Novel> novel = novelRepository.findById(draft.getNovelId());
            draftInfo.put("novelTitle", novel.map(Novel::getTitle).orElse("未知小说"));
            
            result.add(draftInfo);
        }
        
        return result;
    }

    public List<DraftChapter> getNovelDrafts(Long novelId, Long authorId) {
        return draftChapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
    }

    public DraftChapter saveDraft(DraftChapter draft, Long authorId) {
        draft.setAuthorId(authorId);
        draft.setUpdatedAt(new Date());
        if (draft.getCreatedAt() == null) {
            draft.setCreatedAt(new Date());
        }
        return draftChapterRepository.save(draft);
    }

    public void deleteDraft(Long draftId, Long authorId) {
        draftChapterRepository.deleteByIdAndAuthorId(draftId, authorId);
    }

    // ========== 评论管理 ==========

    public List<Comment> getNovelComments(Long novelId, Long authorId) {
        Optional<Novel> novel = novelRepository.findById(novelId);
        if (novel.isPresent() && novel.get().getAuthorId().equals(authorId)) {
            return commentRepository.findByNovelIdOrderByCreatedAtDesc(novelId);
        }
        throw new RuntimeException("无权访问");
    }

    public void deleteComment(Long commentId, Long authorId) {
        Optional<Comment> optional = commentRepository.findById(commentId);
        if (optional.isPresent()) {
            Comment comment = optional.get();
            Optional<Novel> novel = novelRepository.findById(comment.getNovelId());
            if (novel.isPresent() && novel.get().getAuthorId().equals(authorId)) {
                commentRepository.delete(comment);
                return;
            }
        }
        throw new RuntimeException("无权删除此评论");
    }

    // ========== 数据统计 ==========

    public NovelStats getNovelStats(Long novelId, Long authorId) {
        Optional<Novel> novel = novelRepository.findById(novelId);
        if (novel.isPresent() && novel.get().getAuthorId().equals(authorId)) {
            return novelStatsRepository.findByNovelId(novelId)
                .orElse(new NovelStats());
        }
        throw new RuntimeException("无权访问");
    }

    private void updateNovelStats(Long novelId) {
        Optional<NovelStats> optional = novelStatsRepository.findByNovelId(novelId);
        if (optional.isPresent()) {
            NovelStats stats = optional.get();
            List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
            stats.setTotalChapters(chapters.size());
            stats.setTotalWords(chapters.stream().mapToLong(Chapter::getWordCount).sum());
            novelStatsRepository.save(stats);
        }
    }

    public NovelStats getAuthorTotalStats(Long authorId) {
        List<Novel> novels = novelRepository.findByAuthorId(authorId);
        System.out.println("作者 " + authorId + " 有 " + novels.size() + " 部小说");
        NovelStats totalStats = new NovelStats();
        
        for (Novel novel : novels) {
            Optional<NovelStats> stats = novelStatsRepository.findByNovelId(novel.getId());
            if (stats.isPresent()) {
                NovelStats s = stats.get();
                System.out.println("小说 " + novel.getTitle() + " 统计：views=" + s.getTotalViews() + ", likes=" + s.getTotalLikes());
                totalStats.setTotalViews(totalStats.getTotalViews() + s.getTotalViews());
                totalStats.setDailyViews(totalStats.getDailyViews() + s.getDailyViews());
                totalStats.setWeeklyViews(totalStats.getWeeklyViews() + s.getWeeklyViews());
                totalStats.setMonthlyViews(totalStats.getMonthlyViews() + s.getMonthlyViews());
                totalStats.setTotalLikes(totalStats.getTotalLikes() + s.getTotalLikes());
                totalStats.setTotalComments(totalStats.getTotalComments() + s.getTotalComments());
                totalStats.setTotalBookmarks(totalStats.getTotalBookmarks() + s.getTotalBookmarks());
                totalStats.setTotalWords(totalStats.getTotalWords() + s.getTotalWords());
                totalStats.setTotalChapters(totalStats.getTotalChapters() + s.getTotalChapters());
            }
        }
        
        System.out.println("总计：views=" + totalStats.getTotalViews() + ", likes=" + totalStats.getTotalLikes());
        return totalStats;
    }
    
    public Long countNovelsByAuthorId(Long authorId) {
        return novelRepository.countByAuthorId(authorId);
    }
    
    /**
     * 获取作者最近 7 天的阅读趋势数据
     */
    public List<Map<String, Object>> getReadingTrend(Long authorId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        
        // 获取作者的所有小说 ID
        List<Novel> novels = novelRepository.findByAuthorId(authorId);
        if (novels.isEmpty()) {
            return trend;
        }
        
        List<Long> novelIds = novels.stream().map(Novel::getId).toList();
        
        // 计算最近 7 天的日期
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate date = today.minusDays(i);
            String dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 模拟每日阅读量（基于 dailyViews 平均分配）
            // 实际项目中应该有每日统计表
            long totalDailyViews = 0L;
            for (Long novelId : novelIds) {
                Optional<NovelStats> stats = novelStatsRepository.findByNovelId(novelId);
                if (stats.isPresent()) {
                    // 这里简单模拟：假设 dailyViews 是今天的，前几天按比例递减
                    long novelDailyViews = stats.get().getDailyViews() != null ? stats.get().getDailyViews() : 0L;
                    // 模拟前几天数据：越往前越少
                    long simulatedViews = (long) (novelDailyViews * (1.0 - i * 0.1));
                    if (simulatedViews < 0) simulatedViews = 0;
                    totalDailyViews += simulatedViews;
                }
            }
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr);
            dayData.put("views", totalDailyViews);
            trend.add(dayData);
        }
        
        return trend;
    }
}
