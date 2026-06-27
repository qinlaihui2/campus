package com.campus.market.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MarketCommentVO {

    private Long id;
    private Long itemId;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private Long parentId;
    private Long replyToUserId;
    private String replyToUserNickname;
    private String content;
    private Integer likeCount;
    private Boolean liked;
    private LocalDateTime createdAt;
    /** 嵌套子回复 */
    private List<MarketCommentVO> replies;
}
