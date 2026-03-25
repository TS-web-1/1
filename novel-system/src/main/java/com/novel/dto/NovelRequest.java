package com.novel.dto;

import java.util.List;

/**
 * NovelRequest小说请求类 - 小说创建/更新请求参数封装
 * 
 * 该类用于接收小说创建和更新的请求参数，包括：
 * - 小说标题和作者
 * - 小说描述和分类
 * - 标签列表
 * - 封面图片
 * - 小说内容
 * - 连载状态
 */
public class NovelRequest {
    private String title;
    private String author;
    private String description;
    private String category;
    private List<String> tags;
    private String coverImage;
    private String content;
    private String status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
