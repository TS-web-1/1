package com.novel.repository;

import com.novel.model.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {
    List<Novel> findByTitleContaining(String title);

    List<Novel> findByAuthorContaining(String author);

    List<Novel> findByCategory(String category);

    List<Novel> findByStatus(String status);

    Optional<Novel> findByTitleAndAuthor(String title, String author);

    Optional<Novel> findByTitle(String title);

    List<Novel> findByAuthorId(Long authorId);
    
    long countByAuthorId(Long authorId);

    List<Novel> findByReviewStatus(String reviewStatus);

    List<Novel> findByReviewStatusOrReviewStatusIsNull(String reviewStatus);

    @Query("SELECT n FROM Novel n WHERE n.reviewStatus = 'PENDING'")
    List<Novel> findPendingNovels();
    
    @Query("SELECT n FROM Novel n WHERE n.reviewStatus = 'PENDING'")
    Page<Novel> findPendingNovels(Pageable pageable);

    @Query("SELECT n FROM Novel n WHERE n.title LIKE :keyword OR n.author LIKE :keyword OR n.description LIKE :keyword")
    List<Novel> findByTitleContainingOrAuthorContainingOrDescriptionContaining(
            @Param("keyword") String title,
            @Param("keyword") String author,
            @Param("keyword") String description);

    @Query(value = "SELECT * FROM novels WHERE title REGEXP :regex", nativeQuery = true)
    List<Novel> findByTitleRegex(@Param("regex") String regex);

    @Query(value = "SELECT * FROM novels WHERE author = :author AND title REGEXP :regex", nativeQuery = true)
    List<Novel> findByAuthorAndTitleRegex(@Param("author") String author, @Param("regex") String regex);

    int countByCategory(String category);

    /**
     * 批量更新：将连载中字数超过指定值的小说改为完结状态
     */
    @Query("UPDATE Novel n SET n.status = 'completed' WHERE n.status = 'ongoing' AND n.wordCount > :wordCount")
    int updateOngoingToCompleted(@Param("wordCount") Integer wordCount);

    /**
     * 查询连载中字数超过指定值的小说数量
     */
    @Query("SELECT COUNT(n) FROM Novel n WHERE n.status = 'ongoing' AND n.wordCount > :wordCount")
    long countOngoingNovelsWithWordCount(@Param("wordCount") Integer wordCount);
}
