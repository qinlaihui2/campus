package com.campus.notification.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket 推送的通知 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPushVO {

    private Long notificationId;
    private String type;
    private String title;
    private String content;
    private String targetType;
    private Long targetId;
    private LocalDateTime createdAt;
}
