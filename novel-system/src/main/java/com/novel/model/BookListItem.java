package com.novel.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "book_list_items")
public class BookListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_list_id", nullable = false)
    private Long bookListId;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    // 用户在书单中对这本书的评价/备注
    @Column(length = 500)
    private String comment;

    // 排序序号
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "added_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date addedAt;

    public BookListItem() {
        this.addedAt = new Date();
    }

    @PrePersist
    protected void onCreate() {
        addedAt = new Date();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookListId() {
        return bookListId;
    }

    public void setBookListId(Long bookListId) {
        this.bookListId = bookListId;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Date getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Date addedAt) {
        this.addedAt = addedAt;
    }
}
