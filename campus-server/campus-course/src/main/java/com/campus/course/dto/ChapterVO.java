package com.campus.course.dto;

import lombok.Data;

@Data
public class ChapterVO {
    private Long id;
    private String title;
    private String description;
    private String videoUrl;
    private Integer duration;
    private Integer sortOrder;
}
