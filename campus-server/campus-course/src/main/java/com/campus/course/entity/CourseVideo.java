package com.campus.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_video")
public class CourseVideo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("chapter_id")
    private Long chapterId;

    private String title;

    @TableField("video_url")
    private String videoUrl;

    private Integer duration;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;

    private Integer deleted;
}
