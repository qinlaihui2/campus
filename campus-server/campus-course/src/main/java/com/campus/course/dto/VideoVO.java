package com.campus.course.dto;

import lombok.Data;

@Data
public class VideoVO {
    private Long id;
    private String title;
    private String videoUrl;
    private Integer duration;
    private Integer sortOrder;
}
