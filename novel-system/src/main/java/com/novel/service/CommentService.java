package com.novel.service;

import com.novel.model.Comment;
import com.novel.model.CommentLike;
import com.novel.model.NovelStats;
import com.novel.model.User;
import com.novel.repository.CommentLikeRepository;
import com.novel.repository.CommentRepository;
import com.novel.repository.NovelStatsRepository;
import com.novel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NovelStatsRepository novelStatsRepository;

    /**
     * 添加评论
     */
    public Comment addComment(Long userId, Long novelId, Long chapterId, 
                              String content, String commentType, Long parentId) {
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setNovelId(novelId);
        comment.setChapterId(chapterId);
        comment.setContent(content);
        comment.setCommentType(commentType);
        comment.setParentId(parentId);

        Comment savedComment = commentRepository.save(comment);

        if (parentId != null) {
            Comment parentComment = commentRepository.findById(parentId).orElse(null);
            if (parentComment != null) {
                parentComment.setReplyCount(parentComment.getReplyCount() + 1);
                commentRepository.save(parentComment);
            }
        }

        // 更新小说统计表的评论数
        if (novelId != null) {
            novelStatsRepository.findByNovelId(novelId).ifPresent(stats -> {
                stats.setTotalComments(stats.getTotalComments() + 1);
                novelStatsRepository.save(stats);
            });
        }

        fillUserInfo(savedComment);
        return savedComment;
    }

    public Page<Comment> getNovelComments(Long novelId, int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        return fillUserInfo(commentRepository.findByNovelIdAndParentIdIsNullOrderByCreatedAtDesc(novelId, pageable));
    }

    public Page<Comment> getPopularComments(Long novelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return fillUserInfo(commentRepository.findPopularCommentsByNovelId(novelId, pageable));
    }

    public Page<Comment> getChapterComments(Long chapterId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return fillUserInfo(commentRepository.findByChapterIdAndParentIdIsNullOrderByCreatedAtDesc(chapterId, pageable));
    }

    public List<Comment> getCommentReplies(Long commentId) {
        return fillUserInfo(commentRepository.findByParentIdOrderByCreatedAtAsc(commentId));
    }

    public Page<Comment> getUserComments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return fillUserInfo(commentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable));
    }

    public List<Comment> getUserComments(Long userId) {
        return fillUserInfo(commentRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
    }

    /**
     * 点赞评论
     */
    @Transactional
    public boolean likeComment(Long userId, Long commentId) {
        // 检查是否已经点赞
        if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            return false; // 已经点赞过
        }

        CommentLike like = new CommentLike();
        like.setUserId(userId);
        like.setCommentId(commentId);
        commentLikeRepository.save(like);

        // 更新评论的点赞数
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null) {
            comment.setLikesCount(comment.getLikesCount() + 1);
            commentRepository.save(comment);
        }

        return true;
    }

    /**
     * 取消点赞
     */
    @Transactional
    public boolean unlikeComment(Long userId, Long commentId) {
        if (!commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            return false; // 没有点赞过
        }

        commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);

        // 更新评论的点赞数
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null && comment.getLikesCount() > 0) {
            comment.setLikesCount(comment.getLikesCount() - 1);
            commentRepository.save(comment);
        }

        return true;
    }

    /**
     * 检查用户是否点赞过评论
     */
    public boolean hasLiked(Long userId, Long commentId) {
        return commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);
    }

    /**
     * 删除评论
     */
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null || !userId.equals(comment.getUserId())) {
            return false;
        }

        // 获取小说ID用于更新统计
        Long novelId = comment.getNovelId();

        // 删除评论的所有点赞
        commentLikeRepository.deleteByCommentId(commentId);

        // 如果是回复，更新父评论的回复数
        Long parentId = comment.getParentId();
        if (parentId != null) {
            Comment parentComment = commentRepository.findById(parentId).orElse(null);
            if (parentComment != null && parentComment.getReplyCount() != null && parentComment.getReplyCount() > 0) {
                parentComment.setReplyCount(parentComment.getReplyCount() - 1);
                commentRepository.save(parentComment);
            }
        }

        // 删除所有回复
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
        for (Comment reply : replies) {
            commentLikeRepository.deleteByCommentId(reply.getId());
            commentRepository.delete(reply);
        }

        commentRepository.delete(comment);

        // 更新小说统计表的评论数
        if (novelId != null) {
            novelStatsRepository.findByNovelId(novelId).ifPresent(stats -> {
                if (stats.getTotalComments() > 0) {
                    stats.setTotalComments(stats.getTotalComments() - 1);
                    novelStatsRepository.save(stats);
                }
            });
        }

        return true;
    }

    /**
     * 获取评论详情
     */
    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId).orElse(null);
    }

    /**
     * 统计小说的评论数
     */
    public long countNovelComments(Long novelId) {
        return commentRepository.countByNovelId(novelId);
    }

    /**
     * 统计章节的评论数
     */
    public long countChapterComments(Long chapterId) {
        return commentRepository.countByChapterId(chapterId);
    }
    
    /**
     * 获取所有评论
     */
    public List<Comment> getAllComments() {
        List<Comment> comments = commentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return fillUserInfo(comments);
    }

    private void fillUserInfo(Comment comment) {
        if (comment != null && comment.getUserId() != null) {
            userRepository.findById(comment.getUserId()).ifPresent(user -> {
                comment.setUser(new Comment.UserInfo(user.getId(), user.getUsername()));
            });
            List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId());
            for (Comment reply : replies) {
                userRepository.findById(reply.getUserId()).ifPresent(user -> {
                    reply.setUser(new Comment.UserInfo(user.getId(), user.getUsername()));
                });
            }
            comment.setReplies(replies);
        }
    }

    private List<Comment> fillUserInfo(List<Comment> comments) {
        if (comments == null) return comments;
        for (Comment comment : comments) {
            fillUserInfo(comment);
        }
        return comments;
    }

    private Page<Comment> fillUserInfo(Page<Comment> comments) {
        if (comments == null) return comments;
        for (Comment comment : comments) {
            fillUserInfo(comment);
        }
        return comments;
    }
}
