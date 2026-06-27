package com.campus.notification.consumer;

import com.campus.common.notification.NotificationEvent;
import com.campus.notification.entity.Notification;
import com.campus.notification.mapper.NotificationMapper;
import com.campus.notification.websocket.NotificationPushVO;
import com.campus.notification.websocket.NotificationWebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationMapper notificationMapper;

    @RabbitListener(queues = "notification.queue")
    public void handleNotification(NotificationEvent event) {
        log.debug("收到通知事件: type={}, userId={}", event.getType(), event.getUserId());

        // 1. 保存到数据库
        Notification notification = new Notification();
        notification.setUserId(event.getUserId());
        notification.setType(event.getType());
        notification.setTitle(event.getTitle());
        notification.setContent(event.getContent());
        notification.setTargetType(event.getTargetType());
        notification.setTargetId(event.getTargetId());
        notification.setIsRead(false);
        notificationMapper.insert(notification);

        // 2. 实时 WebSocket 推送
        NotificationPushVO pushVO = new NotificationPushVO(
                notification.getId(),
                event.getType(),
                event.getTitle(),
                event.getContent(),
                event.getTargetType(),
                event.getTargetId(),
                notification.getCreatedAt()
        );
        NotificationWebSocketServer.sendToUser(event.getUserId(), pushVO);
    }
}
