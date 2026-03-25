package com.novel.service;

import com.novel.dto.BookmarkRequest;
import com.novel.model.Bookmark;
import com.novel.model.Novel;
import com.novel.model.NovelStats;
import com.novel.model.User;
import com.novel.repository.BookmarkRepository;
import com.novel.repository.NovelRepository;
import com.novel.repository.NovelStatsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final NovelRepository novelRepository;
    private final NovelStatsRepository novelStatsRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, NovelRepository novelRepository, NovelStatsRepository novelStatsRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.novelRepository = novelRepository;
        this.novelStatsRepository = novelStatsRepository;
    }

    public Bookmark createBookmark(BookmarkRequest request, User user) {
        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserIdAndNovelId(
                user.getId(), request.getNovelId());
        
        if (existingBookmark.isPresent()) {
            Bookmark bookmark = existingBookmark.get();
            bookmark.setChapterId(request.getChapterId() != null ? request.getChapterId() : bookmark.getChapterId());
            bookmark.setPosition(request.getPosition() != null ? request.getPosition() : bookmark.getPosition());
            bookmark.setTitle(request.getTitle());
            return bookmarkRepository.save(bookmark);
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(user.getId());
        bookmark.setNovelId(request.getNovelId());
        bookmark.setChapterId(request.getChapterId() != null ? request.getChapterId() : 1L);
        bookmark.setPosition(request.getPosition() != null ? request.getPosition() : 0);
        bookmark.setTitle(request.getTitle());

        Bookmark saved = bookmarkRepository.save(bookmark);

        // 更新 Novel 表的收藏数
        novelRepository.findById(request.getNovelId()).ifPresent(novel -> {
            novel.setBookmarks(novel.getBookmarks() + 1);
            novelRepository.save(novel);
        });

        // 同步更新 NovelStats 表的收藏数
        novelStatsRepository.findByNovelId(request.getNovelId()).ifPresent(stats -> {
            stats.setTotalBookmarks(stats.getTotalBookmarks() + 1L);
            novelStatsRepository.save(stats);
        });

        return saved;
    }

    public List<Bookmark> getUserBookmarks(Long userId) {
        return bookmarkRepository.findByUserId(userId);
    }

    public Optional<Bookmark> getBookmark(Long userId, Long novelId) {
        return bookmarkRepository.findByUserIdAndNovelId(userId, novelId);
    }

    public void deleteBookmark(Long id, Long userId) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("书签不存在"));
        // 如果书签有userId，则检查是否是当前用户；如果没有userId（旧数据），允许删除
        if (bookmark.getUserId() != null && !bookmark.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此书签");
        }
        bookmarkRepository.deleteById(id);
    }

    public Bookmark updateBookmark(Long id, Long userId, BookmarkRequest request) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("书签不存在"));
        
        // 检查是否是当前用户的书签
        if (bookmark.getUserId() != null && !bookmark.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改此书签");
        }
        
        // 更新书签信息
        if (request.getChapterId() != null) {
            bookmark.setChapterId(request.getChapterId());
        }
        if (request.getTitle() != null) {
            bookmark.setTitle(request.getTitle());
        }
        
        return bookmarkRepository.save(bookmark);
    }
}
