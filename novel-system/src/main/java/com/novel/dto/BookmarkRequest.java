package com.novel.dto;

/**
 * BookmarkRequest书签请求类 - 添加书签请求参数封装
 * 
 * 该类用于接收添加书签的请求参数，包括：
 * - 小说ID
 * - 章节ID
 * - 阅读位置（字符位置）
 * - 书签标题
 * - 内容预览
 */
public class BookmarkRequest {
    private Long novelId;
    private Long chapterId;
    private Integer position;
    private String title;
    private String contentPreview;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public void setContentPreview(String contentPreview) {
        this.contentPreview = contentPreview;
    }
}
