package com.example.ebookreader.dto;

public class ChapterDTO {
    private Long id;
    private Integer chapterOrder;
    private String title;
    private String content;

    // Конструктор без параметров
    public ChapterDTO() {
    }

    // Конструктор со всеми параметрами
    public ChapterDTO(Long id, Integer chapterOrder, String title, String content) {
        this.id = id;
        this.chapterOrder = chapterOrder;
        this.title = title;
        this.content = content;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getChapterOrder() {
        return chapterOrder;
    }

    public void setChapterOrder(Integer chapterOrder) {
        this.chapterOrder = chapterOrder;
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

    @Override
    public String toString() {
        return "ChapterDTO{" +
                "id=" + id +
                ", chapterOrder=" + chapterOrder +
                ", title='" + title + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(50, content.length())) + "..." : "null") + '\'' +
                '}';
    }
}