package com.novel.repository;

import com.novel.model.BookListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookListItemRepository extends JpaRepository<BookListItem, Long> {

    // 根据书单ID删除所有项
    void deleteByBookListId(Long bookListId);

    // 根据书单ID获取所有项
    List<BookListItem> findByBookListId(Long bookListId);
}
