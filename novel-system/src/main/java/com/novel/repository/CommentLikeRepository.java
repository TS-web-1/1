package com.novel.repository;

import com.novel.model.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    // 检查用户是否点赞过评论
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    // 获取用户的点赞记录
    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    // 删除点赞记录
    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    // 统计评论的点赞数
    long countByCommentId(Long commentId);

    // 删除评论的所有点赞
    void deleteByCommentId(Long commentId);

    // 获取用户的所有点赞
    List<CommentLike> findByUserId(Long userId);
}
