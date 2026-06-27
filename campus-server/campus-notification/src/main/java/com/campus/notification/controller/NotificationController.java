package com.campus.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.result.PageResult;
import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import com.campus.notification.dto.NotificationVO;
import com.campus.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public R<PageResult<NotificationVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        Page<NotificationVO> result = notificationService.listNotifications(userId, page, size);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/unread-count")
    public R<Map<String, Object>> unreadCount() {
        Long userId = UserContext.getUserId();
        long count = notificationService.getUnreadCount(userId);
        return R.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public R<String> markAsRead(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        notificationService.markAsRead(id, userId);
        return R.ok("已标记为已读");
    }

    @PutMapping("/read-all")
    public R<String> markAllAsRead() {
        Long userId = UserContext.getUserId();
        notificationService.markAllAsRead(userId);
        return R.ok("全部已读");
    }

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        notificationService.deleteNotification(id, userId);
        return R.ok("删除成功");
    }
}
