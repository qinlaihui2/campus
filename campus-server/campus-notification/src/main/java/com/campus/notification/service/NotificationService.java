package com.campus.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.notification.dto.NotificationVO;

public interface NotificationService {

    Page<NotificationVO> listNotifications(Long userId, int page, int size);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long notificationId, Long userId);
}
