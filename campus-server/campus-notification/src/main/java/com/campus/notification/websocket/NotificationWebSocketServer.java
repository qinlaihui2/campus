package com.campus.notification.websocket;

import cn.hutool.json.JSONUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/ws/notification/{userId}")
public class NotificationWebSocketServer {

    private static final Map<Long, Session> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        ONLINE_SESSIONS.put(userId, session);
        log.info("通知 WebSocket 连接成功: userId={}", userId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") Long userId) {
        ONLINE_SESSIONS.remove(userId);
        log.info("通知 WebSocket 断开: userId={}", userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("通知 WebSocket 异常: {}", error.getMessage());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 客户端心跳，暂不处理
    }

    /**
     * 向指定用户推送通知
     */
    public static void sendToUser(Long userId, NotificationPushVO vo) {
        Session session = ONLINE_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(JSONUtil.toJsonStr(vo));
            } catch (IOException e) {
                log.error("通知 WebSocket 推送失败: userId={}", userId, e);
            }
        }
    }
}
