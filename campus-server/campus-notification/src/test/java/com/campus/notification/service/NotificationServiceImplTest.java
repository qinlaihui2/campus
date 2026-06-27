package com.campus.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.exception.BusinessException;
import com.campus.notification.dto.NotificationVO;
import com.campus.notification.entity.Notification;
import com.campus.notification.mapper.NotificationMapper;
import com.campus.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 单元测试")
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        // Inject baseMapper into the parent ServiceImpl
        ReflectionTestUtils.setField(notificationService, "baseMapper", notificationMapper);

        sampleNotification = new Notification();
        sampleNotification.setId(1L);
        sampleNotification.setUserId(100L);
        sampleNotification.setType("LIKE");
        sampleNotification.setTitle("有人赞了你");
        sampleNotification.setContent("测试内容");
        sampleNotification.setTargetType("MARKET");
        sampleNotification.setTargetId(1L);
        sampleNotification.setIsRead(false);
        sampleNotification.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("通知列表查询")
    class ListNotifications {

        @Test
        @DisplayName("分页返回用户通知")
        void shouldReturnPagedNotifications() {
            Page<Notification> page = new Page<>(1, 10);
            page.setRecords(List.of(sampleNotification));
            page.setTotal(1);
            when(notificationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);

            Page<NotificationVO> result = notificationService.listNotifications(100L, 1, 10);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getTitle()).isEqualTo("有人赞了你");
            assertThat(result.getRecords().get(0).getIsRead()).isFalse();
        }
    }

    @Nested
    @DisplayName("未读计数")
    class UnreadCount {

        @Test
        @DisplayName("返回未读通知数量")
        void shouldReturnUnreadCount() {
            when(notificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            long count = notificationService.getUnreadCount(100L);

            assertThat(count).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("标记已读")
    class MarkAsRead {

        @Test
        @DisplayName("成功标记单条已读")
        void shouldMarkSingleAsRead() {
            when(notificationMapper.selectById(1L)).thenReturn(sampleNotification);
            when(notificationMapper.updateById(any(Notification.class))).thenReturn(1);

            assertThatCode(() -> notificationService.markAsRead(1L, 100L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("通知不存在时抛异常")
        void shouldThrowWhenNotificationNotFound() {
            when(notificationMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> notificationService.markAsRead(999L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("通知不存在");
        }

        @Test
        @DisplayName("无权操作他人通知")
        void shouldThrowWhenNotOwner() {
            when(notificationMapper.selectById(1L)).thenReturn(sampleNotification);

            assertThatThrownBy(() -> notificationService.markAsRead(1L, 200L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权限");
        }
    }

    @Nested
    @DisplayName("全部已读 (LambdaUpdateWrapper 需 Spring 上下文，集成测试覆盖)")
    class MarkAllAsRead {
        // markAllAsRead() 使用 LambdaUpdateWrapper.set()，其内部需要 MyBatis-Plus
        // lambda cache，纯 Mockito 单元测试无法初始化。功能正确性由集成测试保证。
    }

    @Nested
    @DisplayName("删除通知")
    class DeleteNotification {

        @Test
        @DisplayName("成功删除自己的通知")
        void shouldDeleteOwnNotification() {
            when(notificationMapper.selectById(1L)).thenReturn(sampleNotification);
            when(notificationMapper.deleteById(1L)).thenReturn(1);

            assertThatCode(() -> notificationService.deleteNotification(1L, 100L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("无权删除他人通知")
        void shouldThrowWhenNotOwner() {
            when(notificationMapper.selectById(1L)).thenReturn(sampleNotification);

            assertThatThrownBy(() -> notificationService.deleteNotification(1L, 200L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权限");
        }
    }
}
