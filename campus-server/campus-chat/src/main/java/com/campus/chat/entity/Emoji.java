package com.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("emoji")
public class Emoji {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("emoji_char")
    private String emojiChar;

    @TableField("image_url")
    private String imageUrl;

    private String tags;

    private String category;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
