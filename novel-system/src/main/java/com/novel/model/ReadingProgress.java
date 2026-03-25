package com.novel.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "reading_progress")
public class ReadingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(name = "chapter_id")
    private Long chapterId;

    // 当前阅读位置（字符位置）
    @Column(name = "position")
    private Integer position = 0;

    // 阅读百分比
    @Column(name = "progress_percent")
    private Double progressPercent = 0.0;

    // 总阅读时长（秒）
    @Column(name = "total_read_time")
    private Integer totalReadTime = 0;

    @Column(name = "last_read_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastReadAt;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public ReadingProgress() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.lastReadAt = new Date();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
        lastReadAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
        lastReadAt = new Date();
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

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Double progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer getTotalReadTime() {
        return totalReadTime;
    }

    public void setTotalReadTime(Integer totalReadTime) {
        this.totalReadTime = totalReadTime;
    }

    public Date getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(Date lastReadAt) {
        this.lastReadAt = lastReadAt;
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
}
