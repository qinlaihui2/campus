package com.campus.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.ResultCode;
import com.campus.notification.dto.NotificationVO;
import com.campus.notification.entity.Notification;
import com.campus.notification.mapper.NotificationMapper;
import com.campus.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public Page<NotificationVO> listNotifications(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);
        Page<Notification> pageParam = new Page<>(page, size);
        Page<Notification> result = this.page(pageParam, wrapper);

        Page<NotificationVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public long getUnreadCount(Long userId) {
        return this.count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false));
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = this.getById(notificationId);
        if (notification == null) {
            throw new BusinessException(ResultCode.NOTIFICATION_NOT_FOUND);
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        notification.setIsRead(true);
        this.updateById(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true);
        this.update(wrapper);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = this.getById(notificationId);
        if (notification == null) {
            throw new BusinessException(ResultCode.NOTIFICATION_NOT_FOUND);
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        this.removeById(notificationId);
    }

    private NotificationVO toVO(Notification entity) {
        NotificationVO vo = new NotificationVO();
        vo.setId(entity.getId());
        vo.setType(entity.getType());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        vo.setIsRead(entity.getIsRead());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
