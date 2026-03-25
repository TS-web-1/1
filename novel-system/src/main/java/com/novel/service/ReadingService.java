package com.novel.service;

import com.novel.model.Bookmark;
import com.novel.model.Chapter;
import com.novel.model.ReadingProgress;
import com.novel.repository.BookmarkRepository;
import com.novel.repository.ChapterRepository;
import com.novel.repository.ReadingProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ReadingService {

    @Autowired
    private ReadingProgressRepository readingProgressRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    /**
     * 保存阅读进度
     */
    public ReadingProgress saveReadingProgress(Long userId, Long novelId, Long chapterId, 
                                                Integer position, Double progressPercent, 
                                                Integer readTime) {
        Optional<ReadingProgress> existingProgress = readingProgressRepository
                .findByUserIdAndNovelId(userId, novelId);

        ReadingProgress progress;
        if (existingProgress.isPresent()) {
            progress = existingProgress.get();
            progress.setChapterId(chapterId);
            progress.setPosition(position);
            progress.setProgressPercent(progressPercent);
            progress.setTotalReadTime(progress.getTotalReadTime() + readTime);
        } else {
            progress = new ReadingProgress();
            progress.setUserId(userId);
            progress.setNovelId(novelId);
            progress.setChapterId(chapterId);
            progress.setPosition(position);
            progress.setProgressPercent(progressPercent);
            progress.setTotalReadTime(readTime);
        }

        return readingProgressRepository.save(progress);
    }

    /**
     * 获取阅读进度
     */
    public ReadingProgress getReadingProgress(Long userId, Long novelId) {
        return readingProgressRepository.findByUserIdAndNovelId(userId, novelId)
                .orElse(null);
    }

    /**
     * 获取用户的阅读历史
     */
    public List<ReadingProgress> getUserReadingHistory(Long userId) {
        return readingProgressRepository.findByUserIdOrderByLastReadAtDesc(userId);
    }

    /**
     * 删除阅读进度
     */
    public void deleteReadingProgress(Long userId, Long novelId) {
        readingProgressRepository.deleteByUserIdAndNovelId(userId, novelId);
    }

    /**
     * 添加书签
     */
    public Bookmark addBookmark(Long userId, Long novelId, Long chapterId, 
                                 String title, Integer position, String contentPreview) {
        // 检查是否已存在相同位置的书签
        Optional<Bookmark> existingBookmark = bookmarkRepository
                .findByUserIdAndNovelIdAndChapterId(userId, novelId, chapterId);

        Bookmark bookmark;
        if (existingBookmark.isPresent()) {
            bookmark = existingBookmark.get();
            bookmark.setTitle(title);
            bookmark.setPosition(position);
            bookmark.setContentPreview(contentPreview);
        } else {
            bookmark = new Bookmark();
            bookmark.setUserId(userId);
            bookmark.setNovelId(novelId);
            bookmark.setChapterId(chapterId);
            bookmark.setTitle(title);
            bookmark.setPosition(position);
            bookmark.setContentPreview(contentPreview);
        }

        return bookmarkRepository.save(bookmark);
    }

    /**
     * 获取用户的书签列表
     */
    public List<Bookmark> getUserBookmarks(Long userId) {
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取特定小说的书签
     */
    public List<Bookmark> getNovelBookmarks(Long userId, Long novelId) {
        return bookmarkRepository.findByUserId(userId).stream()
                .filter(b -> b.getNovelId().equals(novelId))
                .toList();
    }

    /**
     * 删除书签
     */
    public void deleteBookmark(Long userId, Long novelId, Long chapterId) {
        bookmarkRepository.deleteByUserIdAndNovelIdAndChapterId(userId, novelId, chapterId);
    }

    /**
     * 删除书签（通过ID）
     */
    public void deleteBookmarkById(Long bookmarkId, Long userId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new RuntimeException("书签不存在"));
        if (!bookmark.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此书签");
        }
        bookmarkRepository.deleteById(bookmarkId);
    }

    /**
     * 检查是否存在书签
     */
    public boolean hasBookmark(Long userId, Long novelId, Long chapterId) {
        return bookmarkRepository.existsByUserIdAndNovelIdAndChapterId(userId, novelId, chapterId);
    }

    /**
     * 获取章节内容
     */
    public Chapter getChapterContent(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        // 只返回已通过审核的章节（APPROVED）
        if (chapter != null && "APPROVED".equals(chapter.getReviewStatus())) {
            return chapter;
        }
        return null;
    }

    /**
     * 获取章节（别名）
     */
    public Chapter getChapter(Long chapterId) {
        return getChapterContent(chapterId);
    }

    /**
     * 获取小说的所有章节（按章节号排序，同一章节号只保留一条，避免重复）
     * 只显示已通过审核的章节（APPROVED）
     */
    public List<Chapter> getNovelChapters(Long novelId) {
        List<Chapter> list = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        System.out.println("获取小说章节 - novelId: " + novelId + ", 总章节数: " + list.size());
        List<Chapter> result = new ArrayList<>();
        Set<Integer> seenChapterNumbers = new HashSet<>();
        for (Chapter c : list) {
            System.out.println("章节: " + c.getId() + " - " + c.getTitle() + ", 审核状态: " + c.getReviewStatus());
            // 只添加已通过审核的章节（APPROVED）
            if ("APPROVED".equals(c.getReviewStatus())) {
                Integer num = c.getChapterNumber() != null ? c.getChapterNumber() : 0;
                if (seenChapterNumbers.add(num)) {
                    result.add(c);
                }
            }
        }
        System.out.println("已通过审核的章节数: " + result.size());
        return result;
    }

    /**
     * 获取下一章（使用請求的 novelId，並處理 chapterNumber 為 null）
     */
    public Chapter getNextChapter(Long novelId, Long currentChapterId) {
        Chapter currentChapter = chapterRepository.findById(currentChapterId).orElse(null);
        if (currentChapter == null) {
            return null;
        }
        if (!novelId.equals(currentChapter.getNovelId())) {
            return null;
        }
        int currentNum = currentChapter.getChapterNumber() != null ? currentChapter.getChapterNumber() : 0;
        Chapter nextChapter = chapterRepository
                .findByNovelIdAndChapterNumber(novelId, currentNum + 1)
                .orElse(null);
        // 只返回已通过审核的章节
        if (nextChapter != null && "APPROVED".equals(nextChapter.getReviewStatus())) {
            return nextChapter;
        }
        return null;
    }

    /**
     * 获取上一章（使用請求的 novelId，並處理 chapterNumber 為 null）
     */
    public Chapter getPreviousChapter(Long novelId, Long currentChapterId) {
        Chapter currentChapter = chapterRepository.findById(currentChapterId).orElse(null);
        if (currentChapter == null) {
            return null;
        }
        if (!novelId.equals(currentChapter.getNovelId())) {
            return null;
        }
        int currentNum = currentChapter.getChapterNumber() != null ? currentChapter.getChapterNumber() : 0;
        if (currentNum <= 1) {
            return null;
        }
        Chapter previousChapter = chapterRepository
                .findByNovelIdAndChapterNumber(novelId, currentNum - 1)
                .orElse(null);
        // 只返回已通过审核的章节
        if (previousChapter != null && "APPROVED".equals(previousChapter.getReviewStatus())) {
            return previousChapter;
        }
        return null;
    }
}