package com.novel.repository;

import com.novel.model.NovelStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NovelStatsRepository extends JpaRepository<NovelStats, Long> {
    Optional<NovelStats> findByNovelId(Long novelId);
    
    List<NovelStats> findByNovelIdIn(List<Long> novelIds);
}
