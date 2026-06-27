package com.campus.common.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 通知事件发送器 —— 各业务模块注入此 Helper 发送通知事件到 RabbitMQ
 */
@Slf4j
@Component
public class NotificationHelper {

    private static final String NOTIFICATION_QUEUE = "notification.queue";

    private final RabbitTemplate rabbitTemplate;

    public NotificationHelper(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送通知事件（异步，失败不影响主流程）
     */
    public void send(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(NOTIFICATION_QUEUE, event);
            log.debug("通知事件已发送: type={}, userId={}", event.getType(), event.getUserId());
        } catch (Exception e) {
            log.error("通知事件发送失败: type={}, userId={}", event.getType(), event.getUserId(), e);
        }
    }
}
