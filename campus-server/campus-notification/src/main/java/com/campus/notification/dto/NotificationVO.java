package com.campus.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private String targetType;
    private Long targetId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
