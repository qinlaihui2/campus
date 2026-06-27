package com.campus.market.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品列表/详情视图
 */
@Data
public class MarketItemVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String category;
    private String condition;
    private String images;
    /** 解析后的图片 URL 列表（详情用） */
    private List<String> imageUrls;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean liked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
