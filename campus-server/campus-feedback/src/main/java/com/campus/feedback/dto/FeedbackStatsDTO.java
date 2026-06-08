package com.campus.feedback.dto;

import lombok.Data;

@Data
public class FeedbackStatsDTO {
    private Long messageId;
    private Long likeCount;
    private Long dislikeCount;
    private String currentUserFeedback;
}
