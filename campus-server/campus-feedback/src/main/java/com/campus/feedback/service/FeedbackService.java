package com.campus.feedback.service;

import com.campus.feedback.dto.FeedbackStatsDTO;

public interface FeedbackService {

    /**
     * 对消息进行反馈（点赞/踩），同一用户对同一消息仅保留最新一次反馈
     *
     * @return 当前反馈类型：like / dislike / none
     */
    String toggle(Long userId, Long messageId, String feedbackType, String reason);

    /** 获取某条消息的反馈统计及当前用户的反馈状态 */
    FeedbackStatsDTO getStats(Long messageId, Long currentUserId);

    /** 取消对某条消息的反馈 */
    void cancel(Long userId, Long messageId);
}
