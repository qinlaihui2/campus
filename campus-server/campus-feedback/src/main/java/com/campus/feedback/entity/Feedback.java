package com.campus.feedback.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("message_id")
    private Long messageId;

    @TableField("feedback_type")
    private String feedbackType;

    private String reason;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
