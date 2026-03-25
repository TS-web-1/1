package com.novel.repository;

import com.novel.model.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findByUserId(Long userId);

    Optional<Bookmark> findByUserIdAndNovelId(Long userId, Long novelId);

    List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Bookmark> findByNovelId(Long novelId);

    Optional<Bookmark> findByUserIdAndNovelIdAndChapterId(Long userId, Long novelId, Long chapterId);

    void deleteByUserIdAndNovelIdAndChapterId(Long userId, Long novelId, Long chapterId);

    void deleteByUserId(Long userId);

    void deleteByNovelId(Long novelId);

    boolean existsByUserIdAndNovelIdAndChapterId(Long userId, Long novelId, Long chapterId);
}
