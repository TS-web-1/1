package com.novel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Date;

/**
 * Chapter实体类 - 章节信息模型
 * 
 * 该类映射数据库中的chapters表，存储小说章节的完整信息，包括：
 * - 基本信息：章节标题、内容、章节号
 * - 统计数据：字数
 * - 关联信息：所属小说
 * - 审核信息：审核状态、审核意见、审核时间
 */
@Entity
@Table(name = "chapters")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id")
    private Long novelId;

    @ManyToOne
    @JoinColumn(name = "novel_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private Novel novel;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "chapter_number")
    private Integer chapterNumber;

    @Column(name = "word_count")
    private Integer wordCount = 0;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "review_status")
    private String reviewStatus = "PENDING";

    @Column(name = "review_comment")
    private String reviewComment;

    @Column(name = "reviewed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    public Chapter() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.wordCount = 0;
        this.reviewStatus = "PENDING";
    }

    // 用于查询的构造函数
    public Chapter(Long id, Long novelId, String title, Integer chapterNumber, Integer wordCount, String reviewStatus, Date createdAt) {
        this.id = id;
        this.novelId = novelId;
        this.title = title;
        this.chapterNumber = chapterNumber;
        this.wordCount = wordCount;
        this.reviewStatus = reviewStatus;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
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

    @JsonIgnore
    public Novel getNovel() {
        return novel;
    }

    public void setNovel(Novel novel) {
        this.novel = novel;
        if (novel != null) {
            this.novelId = novel.getId();
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}
