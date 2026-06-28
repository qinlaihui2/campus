package com.campus.agent.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.agent.tool.AnnouncementTool;
import com.campus.agent.tool.CourseTool;
import com.campus.agent.tool.LostFoundTool;
import com.campus.agent.tool.MarketTool;
import com.campus.agent.tool.RagTool;
import com.campus.agent.tool.SquareTool;
import com.campus.chat.entity.Conversation;
import com.campus.chat.entity.Message;
import com.campus.chat.mapper.ConversationMapper;
import com.campus.chat.mapper.MessageMapper;
import com.campus.chat.service.ChatService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Agent 核心编排服务。
 * 使用 LangChain4j AiServices + Tool 机制，让 LLM 自主决策调用哪些工具。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final MessageMapper messageMapper;
    private final ChatService chatService;
    private final OpenAiStreamingChatModel streamingChatModel;

    // 所有工具
    private final CourseTool courseTool;
    private final MarketTool marketTool;
    private final LostFoundTool lostFoundTool;
    private final SquareTool squareTool;
    private final AnnouncementTool announcementTool;
    private final RagTool ragTool;

    private final Executor agentExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * SSE 流式 Agent 对话
     */
    public SseEmitter chat(Long userId, Long conversationId, String question) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 分钟超时
        CompletableFuture.runAsync(() -> processChat(userId, conversationId, question, emitter), agentExecutor);
        return emitter;
    }

    private void processChat(Long userId, Long conversationId, String question, SseEmitter emitter) {
        try {
            // 1. 获取或创建会话
            Conversation conversation;
            if (conversationId == null) {
                conversation = chatService.createConversation(userId, truncateTitle(question));
            } else {
                conversation = chatService.getById(conversationId);
                if (conversation == null || !conversation.getUserId().equals(userId)) {
                    sendError(emitter, "会话不存在");
                    return;
                }
            }
            final Long convId = conversation.getId();

            // 2. 保存用户消息
            Message userMessage = new Message();
            userMessage.setConversationId(convId);
            userMessage.setRole("user");
            userMessage.setContent(question);
            userMessage.setCreatedAt(LocalDateTime.now());
            messageMapper.insert(userMessage);

            // 3. 发送会话 ID
            emitter.send(SseEmitter.event()
                    .name("meta")
                    .data("{\"conversationId\":" + convId + "}"));

            // 4. 构建 AiServices Agent（ChatMemoryProvider 为每次请求创建独立 ChatMemory）
            AiChatAssistant assistant = AiServices.builder(AiChatAssistant.class)
                    .streamingChatLanguageModel(streamingChatModel)
                    .chatMemoryProvider(memoryId -> {
                        ChatMemory memory = MessageWindowChatMemory.builder()
                                .maxMessages(20)
                                .build();
                        loadHistory(memory, (Long) memoryId);
                        return memory;
                    })
                    .tools(courseTool, marketTool, lostFoundTool, squareTool,
                            announcementTool, ragTool)
                    .build();

            // 6. 流式调用 Agent
            StringBuilder fullResponse = new StringBuilder();

            TokenStream tokenStream = assistant.chat(convId, question);
            tokenStream.onToolExecuted(toolExecution -> {
                sendToolEvent(emitter, toolExecution.request().name(),
                        toolExecution.request().arguments(), toolExecution.result());
            });
            tokenStream.onPartialResponse(token -> {
                fullResponse.append(token);
                try {
                    emitter.send(SseEmitter.event().name("message").data(token));
                } catch (IOException e) {
                    log.error("SSE message 发送失败", e);
                }
            });
            tokenStream.onCompleteResponse(response -> {
                try {
                    // 保存 AI 回复
                    Message aiMessage = new Message();
                    aiMessage.setConversationId(convId);
                    aiMessage.setRole("assistant");
                    aiMessage.setContent(fullResponse.toString());
                    aiMessage.setCreatedAt(LocalDateTime.now());
                    messageMapper.insert(aiMessage);

                    // 更新会话
                    conversation.setMessageCount(conversation.getMessageCount() + 1);
                    if (conversation.getMessageCount() == 1) {
                        conversation.setTitle(truncateTitle(question));
                    }
                    chatService.updateById(conversation);

                    emitter.send(SseEmitter.event().name("done").data("completed"));
                    emitter.complete();
                } catch (IOException e) {
                    log.error("SSE done 发送失败", e);
                    emitter.completeWithError(e);
                }
            });
            tokenStream.onError(error -> {
                log.error("Agent 调用失败", error);
                sendError(emitter, "AI服务异常: " + error.getMessage());
                emitter.completeWithError(error);
            });
            tokenStream.start();

        } catch (Exception e) {
            log.error("Agent 处理异常", e);
            sendError(emitter, "系统异常: " + e.getMessage());
            emitter.completeWithError(e);
        }
    }

    /**
     * 从 DB 加载历史消息到 ChatMemory
     */
    private void loadHistory(ChatMemory memory, Long conversationId) {
        List<Message> dbMessages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreatedAt));

        for (Message msg : dbMessages) {
            if ("user".equals(msg.getRole())) {
                memory.add(dev.langchain4j.data.message.UserMessage.from(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                memory.add(AiMessage.from(msg.getContent()));
            }
        }
    }

    private void sendToolEvent(SseEmitter emitter, String toolName, String arguments, String result) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("name", toolName);
            data.put("status", "completed");
            data.put("arguments", arguments);
            // 截断过长结果
            if (result != null && result.length() > 200) {
                result = result.substring(0, 200) + "...";
            }
            data.put("result", result);
            emitter.send(SseEmitter.event()
                    .name("tool_call")
                    .data(JSONUtil.toJsonStr(data)));
        } catch (IOException e) {
            log.warn("tool_call 事件发送失败", e);
        }
    }

    private String truncateTitle(String text) {
        if (text == null) return "新对话";
        return text.length() > 30 ? text.substring(0, 30) + "..." : text;
    }

    private void sendError(SseEmitter emitter, String error) {
        try {
            emitter.send(SseEmitter.event().name("error").data(error));
        } catch (IOException ignored) {
        }
    }
}
