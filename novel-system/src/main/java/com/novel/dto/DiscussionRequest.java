package com.novel.dto;

/**
 * DiscussionRequest讨论请求类 - 创建话题/讨论请求参数封装
 * 
 * 该类用于接收创建话题或讨论的请求参数，包括：
 * - 话题标题
 * - 话题内容
 */
public class DiscussionRequest {
    private String title;
    private String content;

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
}
