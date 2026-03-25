package com.novel.dto;

import java.util.Date;

/**
 * ChapterDTO章节数据传输对象
 * 
 * 该类用于在前端和后端之间传输章节数据，包括：
 * - 章节ID和所属小说ID
 * - 章节标题和章节号
 * - 字数统计
 * - 审核状态
 * - 创建时间
 * 
 * 不包含章节内容，用于列表展示
 */
public class ChapterDTO {
    private Long id;
    private Long novelId;
    private String title;
    private Integer chapterNumber;
    private Integer wordCount;
    private String reviewStatus;
    private Date createdAt;

    public ChapterDTO(Long id, Long novelId, String title, Integer chapterNumber, Integer wordCount, String reviewStatus, Date createdAt) {
        this.id = id;
        this.novelId = novelId;
        this.title = title;
        this.chapterNumber = chapterNumber;
        this.wordCount = wordCount;
        this.reviewStatus = reviewStatus;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
