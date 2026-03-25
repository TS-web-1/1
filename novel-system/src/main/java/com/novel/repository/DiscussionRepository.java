package com.novel.repository;

import com.novel.model.Discussion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, Long> {
    List<Discussion> findByNovelId(Long novelId);
    List<Discussion> findByUserId(Long userId);
}
