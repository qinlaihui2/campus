package com.campus.agent.controller;

import com.campus.agent.service.AgentService;
import com.campus.common.annotation.RateLimit;
import com.campus.common.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI Agent 智能助手控制器。
 * Agent 能根据用户意图自主调用工具（课程/二手/失物/广场/公告/知识库等）。
 */
@Tag(name = "AI 智能助手", description = "基于 AI Agent 的智能对话，支持工具调用")
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @RateLimit(key = "agent_chat", permitsPerSecond = 5, message = "提问过于频繁，请稍后再试")
    @Operation(summary = "Agent 智能对话（SSE 流式）")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @RequestParam(required = false) Long conversationId,
            @RequestParam String question) {
        return agentService.chat(UserContext.getUserId(), conversationId, question);
    }
}
