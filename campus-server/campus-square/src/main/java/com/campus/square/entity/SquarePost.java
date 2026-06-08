package com.campus.square.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("square_post")
public class SquarePost extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("message_id")
    private Long messageId;

    private String title;
    private String question;
    private String answer;

    @TableField("references_json")
    private String referencesJson;

    private String category;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("like_count")
    private Integer likeCount;

    private Integer status;
}
