package com.campus.course.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChapterVO {
    private Long id;
    private String title;
    private String description;
    private Integer sortOrder;
    private List<VideoVO> videos;
}
