package com.campus.feedback.controller;

import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import com.campus.feedback.dto.FeedbackStatsDTO;
import com.campus.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /** 点赞/踩消息 */
    @PostMapping("/{messageId}")
    public R<Map<String, String>> feedback(@PathVariable Long messageId,
                                           @RequestParam String type,
                                           @RequestParam(required = false) String reason) {
        Long userId = UserContext.getUserId();
        String result = feedbackService.toggle(userId, messageId, type, reason);
        return R.ok(Map.of("feedback", result));
    }

    /** 获取消息反馈统计 */
    @GetMapping("/stats/{messageId}")
    public R<FeedbackStatsDTO> stats(@PathVariable Long messageId) {
        Long userId = getCurrentUserId();
        return R.ok(feedbackService.getStats(messageId, userId));
    }

    private Long getCurrentUserId() {
        try {
            return UserContext.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
