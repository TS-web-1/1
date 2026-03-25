package com.novel.repository;

import com.novel.model.BookList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookListRepository extends JpaRepository<BookList, Long> {

    // 获取所有公开书单
    Page<BookList> findByListTypeOrderByCreatedAtDesc(String listType, Pageable pageable);

    // 获取用户的书单
    List<BookList> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 搜索书单
    Page<BookList> findByTitleContainingOrDescriptionContainingAndListTypeOrderByCreatedAtDesc(
            String title, String description, String listType, Pageable pageable);

    // 获取热门书单
    Page<BookList> findByListTypeOrderByCollectCountDesc(String listType, Pageable pageable);
}
