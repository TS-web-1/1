package com.novel.repository;

import com.novel.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 获取小说的所有评论（不包括回复）
    Page<Comment> findByNovelIdAndParentIdIsNullOrderByCreatedAtDesc(Long novelId, Pageable pageable);

    // 获取小说的热门评论
    @Query("SELECT c FROM Comment c WHERE c.novelId = :novelId AND c.parentId IS NULL ORDER BY c.likesCount DESC, c.createdAt DESC")
    Page<Comment> findPopularCommentsByNovelId(@Param("novelId") Long novelId, Pageable pageable);

    // 获取章节的评论
    Page<Comment> findByChapterIdAndParentIdIsNullOrderByCreatedAtDesc(Long chapterId, Pageable pageable);

    // 获取评论的所有回复
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    // 获取用户的所有评论
    Page<Comment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 获取用户的所有评论（不分页）
    List<Comment> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // 统计小说的评论数
    long countByNovelId(Long novelId);

    // 统计章节的评论数
    long countByChapterId(Long chapterId);

    // 删除小说的所有评论
    void deleteByNovelId(Long novelId);

    // 按创建时间降序查找小说评论
    List<Comment> findByNovelIdOrderByCreatedAtDesc(Long novelId);
}
