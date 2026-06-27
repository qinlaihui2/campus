package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("market_offer")
public class MarketOffer extends BaseEntity {

    @TableField("item_id")
    private Long itemId;

    @TableField("buyer_id")
    private Long buyerId;

    @TableField("seller_id")
    private Long sellerId;

    private BigDecimal price;

    private String message;

    /** PENDING, ACCEPTED, REJECTED, CANCELLED */
    private String status;
}
