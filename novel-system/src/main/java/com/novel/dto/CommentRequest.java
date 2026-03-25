package com.novel.dto;

/**
 * CommentRequest评论请求类 - 发表评论请求参数封装
 * 
 * 该类用于接收发表评论的请求参数，包括：
 * - 小说ID
 * - 评论内容
 */
public class CommentRequest {
    private Long novelId;
    private String content;

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
