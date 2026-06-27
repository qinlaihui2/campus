package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("market_like")
public class MarketLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("item_id")
    private Long itemId;

    @TableField("user_id")
    private Long userId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
