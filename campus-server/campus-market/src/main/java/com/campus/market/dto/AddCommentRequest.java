package com.campus.market.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    private String content;

    /** 父评论ID（回复时传） */
    private Long parentId;

    /** 回复目标用户ID */
    private Long replyToUserId;
}
