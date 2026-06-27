package com.campus.market.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OfferVO {

    private Long id;
    private Long itemId;
    private String itemTitle;
    private Long buyerId;
    private String buyerName;
    private String buyerAvatar;
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;
    private BigDecimal price;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
