package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("market_item")
public class MarketItem extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    private String title;

    private String description;

    private BigDecimal price;

    @TableField("original_price")
    private BigDecimal originalPrice;

    private String category;

    @TableField("`condition`")
    private String condition;

    /** JSON 数组字符串，存储图片 URL 列表 */
    private String images;

    /** ON_SALE, SOLD, REMOVED */
    private String status;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("like_count")
    private Integer likeCount;
}
