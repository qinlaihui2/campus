package com.campus.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseVO {
    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private String instructor;
    private String category;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer chapterCount;
    private Boolean liked;
    private Boolean favorited;
    private LocalDateTime createdAt;
}
