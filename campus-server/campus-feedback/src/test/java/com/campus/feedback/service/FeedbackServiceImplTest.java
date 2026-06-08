package com.campus.feedback.service;

import com.campus.common.exception.BusinessException;
import com.campus.feedback.dto.FeedbackStatsDTO;
import com.campus.feedback.entity.Feedback;
import com.campus.feedback.mapper.FeedbackMapper;
import com.campus.feedback.service.impl.FeedbackServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("反馈服务单元测试")
class FeedbackServiceImplTest {

    @Mock private FeedbackMapper feedbackMapper;
    @InjectMocks private FeedbackServiceImpl feedbackService;

    @Nested @DisplayName("Toggle 反馈")
    class Toggle {
        @Test @DisplayName("首次点赞返回 like")
        void shouldCreateLike() {
            when(feedbackMapper.selectOne(any())).thenReturn(null);
            when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);

            String result = feedbackService.toggle(1L, 100L, "like", null);
            assertThat(result).isEqualTo("like");
        }

        @Test @DisplayName("相同反馈 → 取消返回 none")
        void shouldToggleOff() {
            Feedback existing = new Feedback();
            existing.setId(1L); existing.setUserId(1L);
            existing.setMessageId(100L); existing.setFeedbackType("like");
            when(feedbackMapper.selectOne(any())).thenReturn(existing);
            when(feedbackMapper.deleteById(1L)).thenReturn(1);

            String result = feedbackService.toggle(1L, 100L, "like", null);
            assertThat(result).isEqualTo("none");
        }

        @Test @DisplayName("dislike → like 切换返回 like")
        void shouldSwitchFromDislike() {
            Feedback existing = new Feedback();
            existing.setId(1L); existing.setUserId(1L);
            existing.setMessageId(100L); existing.setFeedbackType("dislike");
            when(feedbackMapper.selectOne(any())).thenReturn(existing);
            when(feedbackMapper.updateById(any(Feedback.class))).thenReturn(1);

            String result = feedbackService.toggle(1L, 100L, "like", "有更好的答案");
            assertThat(result).isEqualTo("like");
        }

        @Test @DisplayName("无效反馈类型抛出异常")
        void shouldRejectInvalidType() {
            assertThatThrownBy(() -> feedbackService.toggle(1L, 100L, "invalid", null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested @DisplayName("反馈统计")
    class Stats {
        @Test @DisplayName("有反馈时返回正确计数")
        void shouldReturnStats() {
            when(feedbackMapper.countByType(100L)).thenReturn(List.of(
                    Map.of("feedback_type", "like", "count", 5L),
                    Map.of("feedback_type", "dislike", "count", 2L)
            ));

            Feedback existing = new Feedback();
            existing.setFeedbackType("like");
            when(feedbackMapper.selectOne(any())).thenReturn(existing);

            FeedbackStatsDTO stats = feedbackService.getStats(100L, 1L);
            assertThat(stats.getLikeCount()).isEqualTo(5);
            assertThat(stats.getDislikeCount()).isEqualTo(2);
            assertThat(stats.getCurrentUserFeedback()).isEqualTo("like");
        }

        @Test @DisplayName("无反馈时返回全零")
        void shouldReturnZeroStats() {
            when(feedbackMapper.countByType(100L)).thenReturn(List.of());
            when(feedbackMapper.selectOne(any())).thenReturn(null);

            FeedbackStatsDTO stats = feedbackService.getStats(100L, 1L);
            assertThat(stats.getLikeCount()).isZero();
            assertThat(stats.getDislikeCount()).isZero();
            assertThat(stats.getCurrentUserFeedback()).isNull();
        }
    }

    @Nested @DisplayName("取消反馈")
    class Cancel {
        @Test @DisplayName("取消反馈调用 delete")
        void shouldDelete() {
            when(feedbackMapper.delete(any())).thenReturn(1);
            assertThatCode(() -> feedbackService.cancel(1L, 100L)).doesNotThrowAnyException();
        }
    }
}
