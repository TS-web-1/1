package com.novel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CrawlRuleDTO {

    private String bookListUrl;
    private Map<String, String> catIdRule;
    private String bookIdPatten;
    private String pagePatten;
    private String totalPagePatten;
    private String bookDetailUrl;
    private String bookNamePatten;
    private String authorNamePatten;
    private String picUrlPatten;
    private String picUrlPrefix;
    private String statusPatten;
    private Map<String, Integer> bookStatusRule;
    private String visitCountPatten;
    private String descStart;
    private String descEnd;
    private String bookIndexUrl;
    private String bookIndexStart;
    private String indexIdPatten;
    private String indexNamePatten;
    private String bookContentUrl;
    private String contentStart;
    private String contentEnd;

    public String getBookListUrl() {
        return bookListUrl;
    }

    public void setBookListUrl(String bookListUrl) {
        this.bookListUrl = bookListUrl;
    }

    public Map<String, String> getCatIdRule() {
        return catIdRule;
    }

    public void setCatIdRule(Map<String, String> catIdRule) {
        this.catIdRule = catIdRule;
    }

    public String getBookIdPatten() {
        return bookIdPatten;
    }

    public void setBookIdPatten(String bookIdPatten) {
        this.bookIdPatten = bookIdPatten;
    }

    public String getPagePatten() {
        return pagePatten;
    }

    public void setPagePatten(String pagePatten) {
        this.pagePatten = pagePatten;
    }

    public String getTotalPagePatten() {
        return totalPagePatten;
    }

    public void setTotalPagePatten(String totalPagePatten) {
        this.totalPagePatten = totalPagePatten;
    }

    public String getBookDetailUrl() {
        return bookDetailUrl;
    }

    public void setBookDetailUrl(String bookDetailUrl) {
        this.bookDetailUrl = bookDetailUrl;
    }

    public String getBookNamePatten() {
        return bookNamePatten;
    }

    public void setBookNamePatten(String bookNamePatten) {
        this.bookNamePatten = bookNamePatten;
    }

    public String getAuthorNamePatten() {
        return authorNamePatten;
    }

    public void setAuthorNamePatten(String authorNamePatten) {
        this.authorNamePatten = authorNamePatten;
    }

    public String getPicUrlPatten() {
        return picUrlPatten;
    }

    public void setPicUrlPatten(String picUrlPatten) {
        this.picUrlPatten = picUrlPatten;
    }

    public String getPicUrlPrefix() {
        return picUrlPrefix;
    }

    public void setPicUrlPrefix(String picUrlPrefix) {
        this.picUrlPrefix = picUrlPrefix;
    }

    public String getStatusPatten() {
        return statusPatten;
    }

    public void setStatusPatten(String statusPatten) {
        this.statusPatten = statusPatten;
    }

    public Map<String, Integer> getBookStatusRule() {
        return bookStatusRule;
    }

    public void setBookStatusRule(Map<String, Integer> bookStatusRule) {
        this.bookStatusRule = bookStatusRule;
    }

    public String getVisitCountPatten() {
        return visitCountPatten;
    }

    public void setVisitCountPatten(String visitCountPatten) {
        this.visitCountPatten = visitCountPatten;
    }

    public String getDescStart() {
        return descStart;
    }

    public void setDescStart(String descStart) {
        this.descStart = descStart;
    }

    public String getDescEnd() {
        return descEnd;
    }

    public void setDescEnd(String descEnd) {
        this.descEnd = descEnd;
    }

    public String getBookIndexUrl() {
        return bookIndexUrl;
    }

    public void setBookIndexUrl(String bookIndexUrl) {
        this.bookIndexUrl = bookIndexUrl;
    }

    public String getBookIndexStart() {
        return bookIndexStart;
    }

    public void setBookIndexStart(String bookIndexStart) {
        this.bookIndexStart = bookIndexStart;
    }

    public String getIndexIdPatten() {
        return indexIdPatten;
    }

    public void setIndexIdPatten(String indexIdPatten) {
        this.indexIdPatten = indexIdPatten;
    }

    public String getIndexNamePatten() {
        return indexNamePatten;
    }

    public void setIndexNamePatten(String indexNamePatten) {
        this.indexNamePatten = indexNamePatten;
    }

    public String getBookContentUrl() {
        return bookContentUrl;
    }

    public void setBookContentUrl(String bookContentUrl) {
        this.bookContentUrl = bookContentUrl;
    }

    public String getContentStart() {
        return contentStart;
    }

    public void setContentStart(String contentStart) {
        this.contentStart = contentStart;
    }

    public String getContentEnd() {
        return contentEnd;
    }

    public void setContentEnd(String contentEnd) {
        this.contentEnd = contentEnd;
    }
}
