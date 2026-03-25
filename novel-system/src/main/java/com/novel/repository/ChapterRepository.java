package com.novel.repository;

import com.novel.model.Chapter;
import com.novel.model.Novel;
import com.novel.dto.ChapterDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByNovelId(Long novelId);

    List<Chapter> findByNovelIdOrderByChapterNumberAsc(Long novelId);

    // 根据小说ID和章节号查找章节
    Optional<Chapter> findByNovelIdAndChapterNumber(Long novelId, Integer chapterNumber);

    // 统计小说的章节数量
    int countByNovelId(Long novelId);

    // 删除小说的所有章节
    void deleteByNovelId(Long novelId);
    
    // 基于Novel对象的查询方法
    List<Chapter> findByNovel(Novel novel);
    
    List<Chapter> findByNovelOrderByChapterNumberAsc(Novel novel);
    
    Optional<Chapter> findByNovelAndChapterNumber(Novel novel, Integer chapterNumber);
    
    void deleteByNovel(Novel novel);
    
    // 根据审核状态查找章节
    List<Chapter> findByReviewStatus(String reviewStatus);
    
    // 分页根据审核状态查找章节
    Page<Chapter> findByReviewStatus(String reviewStatus, Pageable pageable);
    
    // 查找待审核的章节（PENDING）
    @Query("SELECT c FROM Chapter c WHERE c.reviewStatus = 'PENDING' OR c.reviewStatus IS NULL")
    List<Chapter> findPendingChapters();
    
    // 分页查找待审核的章节（PENDING）
    @Query("SELECT new com.novel.dto.ChapterDTO(c.id, c.novelId, c.title, c.chapterNumber, c.wordCount, c.reviewStatus, c.createdAt) FROM Chapter c WHERE c.reviewStatus = 'PENDING' OR c.reviewStatus IS NULL")
    Page<ChapterDTO> findPendingChapters(Pageable pageable);
    
    // 批量更新所有章节为通过状态
    @Modifying
    @Transactional
    @Query("UPDATE Chapter c SET c.reviewStatus = 'APPROVED', c.reviewComment = NULL, c.reviewedAt = CURRENT_TIMESTAMP WHERE c.reviewStatus IS NULL OR c.reviewStatus != 'APPROVED'")
    int updateAllChaptersToApproved();
}
