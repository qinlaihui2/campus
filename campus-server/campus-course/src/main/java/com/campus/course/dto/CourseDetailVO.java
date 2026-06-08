package com.campus.course.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CourseDetailVO extends CourseVO {
    private List<ChapterVO> chapters;
}
