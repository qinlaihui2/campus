package com.campus.announcement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("announcement")
public class Announcement extends BaseEntity {

    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private String category;
    private Integer priority;
    private Integer isCarousel;
    private Integer carouselSort;
    private String status;
    private Long publisherId;
    private LocalDateTime publishedAt;
}
