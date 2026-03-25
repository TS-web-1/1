package com.novel.repository;

import com.novel.model.NovelLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NovelLikeRepository extends JpaRepository<NovelLike, Long> {
    
    Optional<NovelLike> findByUserIdAndNovelId(Long userId, Long novelId);
    
    @Query("SELECT COUNT(nl) FROM NovelLike nl WHERE nl.novelId = ?1")
    Long countByNovelId(Long novelId);
    
    boolean existsByUserIdAndNovelId(Long userId, Long novelId);
}
