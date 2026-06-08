package com.campus.chat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import
        com.campus.chat.entity.Conversation;
import com.campus.chat.entity.Message;
import com.campus.chat.service.ChatService;
import com.campus.common.annotation.RateLimit;
import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "AI 对话", description = "SSE 流式聊天、会话管理、消息反馈")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * SSE 流式对话（每人每秒最多 5 次）
     */
    @RateLimit(key = "chat_send", permitsPerSecond = 5, message = "提问过于频繁，请稍后再试")
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter send(@RequestParam(required = false) Long conversationId,
                           @RequestParam String question) {
        return chatService.chat(UserContext.getUserId(), conversationId, question);
    }

    /**
     * 新建会话
     */
    @PostMapping("/conversation")
    public R<Conversation> createConversation(@RequestParam(required = false) String title) {
        return R.ok(chatService.createConversation(UserContext.getUserId(), title));
    }

    /**
     * 会话列表
     */
    @GetMapping("/conversations")
    public R<Page<Conversation>> conversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(chatService.getConversations(UserContext.getUserId(), page, size));
    }

    /**
     * 会话消息列表
     */
    @GetMapping("/messages/{conversationId}")
    public R<List<Message>> messages(@PathVariable Long conversationId) {
        return R.ok(chatService.getMessages(conversationId, UserContext.getUserId()));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversation/{conversationId}")
    public R<String> deleteConversation(@PathVariable Long conversationId) {
        chatService.deleteConversation(conversationId, UserContext.getUserId());
        return R.ok("删除成功");
    }

    /**
     * 消息反馈（点赞/踩）
     */
    @PostMapping("/feedback/{messageId}")
    public R<String> feedback(@PathVariable Long messageId, @RequestParam String feedback) {
        chatService.feedback(messageId, feedback);
        return R.ok("反馈成功");
    }
}
