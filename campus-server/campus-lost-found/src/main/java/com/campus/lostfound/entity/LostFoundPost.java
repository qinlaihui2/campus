package com.campus.lostfound.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lost_found_post")
public class LostFoundPost extends BaseEntity {

    private String type;
    private String title;
    private String description;
    private String category;
    private String location;
    private String contactWay;
    private String imageUrls;
    private String status;
    private Long publisherId;
    private LocalDateTime publishedAt;
}
