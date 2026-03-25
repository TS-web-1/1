package com.novel.repository;

import com.novel.model.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {

    // 查找用户的阅读进度
    Optional<ReadingProgress> findByUserIdAndNovelId(Long userId, Long novelId);

    // 获取用户的所有阅读进度
    List<ReadingProgress> findByUserIdOrderByLastReadAtDesc(Long userId);

    // 获取用户最近阅读的进度
    List<ReadingProgress> findTop10ByUserIdOrderByLastReadAtDesc(Long userId);

    // 删除用户的阅读进度
    void deleteByUserIdAndNovelId(Long userId, Long novelId);

    // 删除小说的所有阅读进度
    void deleteByNovelId(Long novelId);

    // 统计小说的阅读人数
    long countByNovelId(Long novelId);
}
