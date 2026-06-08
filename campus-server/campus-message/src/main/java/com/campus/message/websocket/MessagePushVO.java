package com.campus.message.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessagePushVO {
    private String type;         // NEW_MESSAGE / UNREAD_COUNT
    private Long conversationId;
    private Long senderId;
    private String content;
    private String messageType;  // TEXT / IMAGE
    private Integer unreadCount;
    private LocalDateTime createdAt;
}
