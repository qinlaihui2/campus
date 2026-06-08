package com.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {

    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    private String role;
    private String content;

    @TableField("references_json")
    private String referencesJson;

    @TableField("token_count")
    private Integer tokenCount;

    private String feedback;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
