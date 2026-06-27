package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("market_comment")
public class MarketComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("item_id")
    private Long itemId;

    @TableField("user_id")
    private Long userId;

    @TableField("parent_id")
    private Long parentId;

    @TableField("reply_to_user_id")
    private Long replyToUserId;

    private String content;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    private Integer deleted;
}
