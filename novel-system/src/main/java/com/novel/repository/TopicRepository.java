package com.novel.repository;

import com.novel.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    // 获取所有话题（按置顶和时间排序）
    Page<Topic> findAllByOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);

    // 按分类获取话题
    Page<Topic> findByCategoryOrderByIsPinnedDescCreatedAtDesc(String category, Pageable pageable);

    // 获取用户发布的话题
    Page<Topic> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 搜索话题
    Page<Topic> findByTitleContainingOrContentContainingOrderByCreatedAtDesc(
            String title, String content, Pageable pageable);

    // 获取置顶话题
    List<Topic> findByIsPinnedTrueOrderByCreatedAtDesc();
}
