package com.campus.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("im_message")
public class Message implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private String type;
    private Integer isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
