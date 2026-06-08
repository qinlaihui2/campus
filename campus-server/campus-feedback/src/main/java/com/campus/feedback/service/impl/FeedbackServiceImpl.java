package com.campus.feedback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.ResultCode;
import com.campus.feedback.dto.FeedbackStatsDTO;
import com.campus.feedback.entity.Feedback;
import com.campus.feedback.mapper.FeedbackMapper;
import com.campus.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackMapper feedbackMapper;

    @Override
    @Transactional
    public String toggle(Long userId, Long messageId, String feedbackType, String reason) {
        if (!"like".equals(feedbackType) && !"dislike".equals(feedbackType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        // 查找用户对此消息的现有反馈
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, userId)
                .eq(Feedback::getMessageId, messageId);
        Feedback existing = feedbackMapper.selectOne(wrapper);

        if (existing != null) {
            if (feedbackType.equals(existing.getFeedbackType())) {
                // 点击相同的反馈 → 取消
                feedbackMapper.deleteById(existing.getId());
                return "none";
            } else {
                // 切换反馈类型 (like ↔ dislike)
                existing.setFeedbackType(feedbackType);
                existing.setReason(reason);
                existing.setCreatedAt(LocalDateTime.now());
                feedbackMapper.updateById(existing);
                return feedbackType;
            }
        }

        // 新建反馈
        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setMessageId(messageId);
        fb.setFeedbackType(feedbackType);
        fb.setReason(reason);
        fb.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(fb);
        return feedbackType;
    }

    @Override
    public FeedbackStatsDTO getStats(Long messageId, Long currentUserId) {
        FeedbackStatsDTO dto = new FeedbackStatsDTO();
        dto.setMessageId(messageId);

        List<Map<String, Object>> counts = feedbackMapper.countByType(messageId);
        long likeCount = 0, dislikeCount = 0;
        for (Map<String, Object> row : counts) {
            String type = (String) row.get("feedback_type");
            long count = ((Number) row.get("count")).longValue();
            if ("like".equals(type)) likeCount = count;
            else if ("dislike".equals(type)) dislikeCount = count;
        }
        dto.setLikeCount(likeCount);
        dto.setDislikeCount(dislikeCount);

        if (currentUserId != null) {
            LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<Feedback>()
                    .eq(Feedback::getUserId, currentUserId)
                    .eq(Feedback::getMessageId, messageId);
            Feedback existing = feedbackMapper.selectOne(wrapper);
            dto.setCurrentUserFeedback(existing != null ? existing.getFeedbackType() : null);
        }

        return dto;
    }

    @Override
    public void cancel(Long userId, Long messageId) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, userId)
                .eq(Feedback::getMessageId, messageId);
        feedbackMapper.delete(wrapper);
    }
}
