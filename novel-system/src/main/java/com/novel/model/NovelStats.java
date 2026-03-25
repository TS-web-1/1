package com.novel.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "novel_stats")
public class NovelStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "novel_id", nullable = false, unique = true)
    private Long novelId;
    
    @Column(name = "total_views")
    private Long totalViews = 0L;
    
    @Column(name = "daily_views")
    private Long dailyViews = 0L;
    
    @Column(name = "weekly_views")
    private Long weeklyViews = 0L;
    
    @Column(name = "monthly_views")
    private Long monthlyViews = 0L;
    
    @Column(name = "total_likes")
    private Long totalLikes = 0L;
    
    @Column(name = "total_comments")
    private Long totalComments = 0L;
    
    @Column(name = "total_bookmarks")
    private Long totalBookmarks = 0L;
    
    @Column(name = "total_words")
    private Long totalWords = 0L;
    
    @Column(name = "total_chapters")
    private Integer totalChapters = 0;
    
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getNovelId() { return novelId; }
    public void setNovelId(Long novelId) { this.novelId = novelId; }
    
    public Long getTotalViews() { return totalViews; }
    public void setTotalViews(Long totalViews) { this.totalViews = totalViews; }
    
    public Long getDailyViews() { return dailyViews; }
    public void setDailyViews(Long dailyViews) { this.dailyViews = dailyViews; }
    
    public Long getWeeklyViews() { return weeklyViews; }
    public void setWeeklyViews(Long weeklyViews) { this.weeklyViews = weeklyViews; }
    
    public Long getMonthlyViews() { return monthlyViews; }
    public void setMonthlyViews(Long monthlyViews) { this.monthlyViews = monthlyViews; }
    
    public Long getTotalLikes() { return totalLikes; }
    public void setTotalLikes(Long totalLikes) { this.totalLikes = totalLikes; }
    
    public Long getTotalComments() { return totalComments; }
    public void setTotalComments(Long totalComments) { this.totalComments = totalComments; }
    
    public Long getTotalBookmarks() { return totalBookmarks; }
    public void setTotalBookmarks(Long totalBookmarks) { this.totalBookmarks = totalBookmarks; }
    
    public Long getTotalWords() { return totalWords; }
    public void setTotalWords(Long totalWords) { this.totalWords = totalWords; }
    
    public Integer getTotalChapters() { return totalChapters; }
    public void setTotalChapters(Integer totalChapters) { this.totalChapters = totalChapters; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
