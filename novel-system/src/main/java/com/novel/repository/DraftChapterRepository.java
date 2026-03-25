package com.novel.repository;

import com.novel.model.DraftChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DraftChapterRepository extends JpaRepository<DraftChapter, Long> {
    List<DraftChapter> findByAuthorIdOrderByUpdatedAtDesc(Long authorId);
    List<DraftChapter> findByNovelIdOrderByChapterNumberAsc(Long novelId);
    Optional<DraftChapter> findByIdAndAuthorId(Long id, Long authorId);
    void deleteByIdAndAuthorId(Long id, Long authorId);
}
