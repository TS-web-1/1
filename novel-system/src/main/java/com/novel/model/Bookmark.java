package com.novel.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Bookmark实体类 - 书签/收藏信息模型
 * 
 * 该类映射数据库中的bookmarks表，存储用户的书签和收藏信息，包括：
 * - 关联的用户和小说
 * - 阅读位置（章节ID和字符位置）
 * - 书签备注和内容预览
 */
@Entity
@Table(name = "bookmarks")
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "novel_id", insertable = false, updatable = false)
    private Novel novel;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "title")
    private String title;

    @Column(name = "position")
    private Integer position = 0;

    @Column(name = "content_preview", length = 500)
    private String contentPreview;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Bookmark() {
        this.createdAt = new Date();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public Novel getNovel() {
        return novel;
    }

    public void setNovel(Novel novel) {
        this.novel = novel;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public void setContentPreview(String contentPreview) {
        this.contentPreview = contentPreview;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
