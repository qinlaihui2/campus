package com.campus.common.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通知事件 DTO —— 通过 RabbitMQ 在模块间传递
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通知类型：LIKE, COMMENT, REPLY, SYSTEM, OFFER */
    private String type;

    /** 接收通知的用户 ID */
    private Long userId;

    private String title;

    private String content;

    /** 关联的目标类型：POST, COURSE, MARKET, MESSAGE, COMMENT */
    private String targetType;

    private Long targetId;
}
