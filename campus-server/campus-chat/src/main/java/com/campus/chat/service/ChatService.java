package com.campus.chat.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.chat.entity.Conversation;
import com.campus.chat.entity.Message;
import com.campus.chat.mapper.ConversationMapper;
import com.campus.chat.mapper.MessageMapper;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.ResultCode;
import com.campus.feedback.service.FeedbackService;
import com.campus.rag.llm.RagService;
import com.campus.rag.retriever.RetrievalResult;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService extends ServiceImpl<ConversationMapper, Conversation> {

    private final MessageMapper messageMapper;
    private final RagService ragService;
    private final FeedbackService feedbackService;

    /**
     * 创建新会话
     */
    @Transactional
    public Conversation createConversation(Long userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(title != null ? title : "新对话");
        conversation.setMessageCount(0);
        conversation.setStatus(1);
        this.save(conversation);
        return conversation;
    }

    /**
     * SSE 流式对话
     */
    public SseEmitter chat(Long userId, Long conversationId, String question) {
        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> {
            try {
                // 1. 获取或创建会话
                Conversation conversation;
                if (conversationId == null) {
                    conversation = createConversation(userId, truncateTitle(question));
                } else {
                    conversation = this.getById(conversationId);
                    if (conversation == null || !conversation.getUserId().equals(userId)) {
                        sendError(emitter, "会话不存在");
                        return;
                    }
                }

                // 2. 保存用户消息
                Message userMessage = new Message();
                userMessage.setConversationId(conversation.getId());
                userMessage.setRole("user");
                userMessage.setContent(question);
                userMessage.setCreatedAt(LocalDateTime.now());
                messageMapper.insert(userMessage);

                // 3. 获取对话历史（最近10轮）
                String history = buildConversationHistory(conversation.getId());

                // 4. RAG 检索
                List<RetrievalResult> retrievalResults = ragService.retrieve(question);

                // 5. 构建 Prompt
                String prompt = ragService.buildPrompt(question, history, retrievalResults);

                // 6. 发送会话 ID
                emitter.send(SseEmitter.event()
                        .name("meta")
                        .data("{\"conversationId\":" + conversation.getId() + "}"));

                // 7. 发送参考来源
                if (!retrievalResults.isEmpty()) {
                    String refsJson = JSONUtil.toJsonStr(
                            retrievalResults.stream()
                                    .map(r -> r.getContent().substring(0, Math.min(100, r.getContent().length())) + "...")
                                    .collect(Collectors.toList()));
                    emitter.send(SseEmitter.event().name("references").data(refsJson));
                }

                // 8. 流式调用 LLM
                StringBuilder fullResponse = new StringBuilder();
                OpenAiStreamingChatModel model = ragService.getStreamingChatModel();

                model.generate(prompt, new dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        try {
                            fullResponse.append(token);
                            emitter.send(SseEmitter.event().name("message").data(token));
                        } catch (IOException e) {
                            log.error("SSE发送失败", e);
                        }
                    }

                    @Override
                    public void onComplete(dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response) {
                        try {
                            // 保存 AI 回复
                            Message aiMessage = new Message();
                            aiMessage.setConversationId(conversation.getId());
                            aiMessage.setRole("assistant");
                            aiMessage.setContent(fullResponse.toString());
                            aiMessage.setReferencesJson(JSONUtil.toJsonStr(retrievalResults));
                            aiMessage.setCreatedAt(LocalDateTime.now());
                            messageMapper.insert(aiMessage);

                            // 更新会话消息数
                            conversation.setMessageCount(conversation.getMessageCount() + 1);
                            if (conversation.getMessageCount() == 1) {
                                conversation.setTitle(truncateTitle(question));
                            }
                            updateById(conversation);

                            emitter.send(SseEmitter.event().name("done").data("completed"));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("SSE完成事件发送失败", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("LLM调用失败", error);
                        try {
                            sendError(emitter, "AI服务异常: " + error.getMessage());
                        } finally {
                            emitter.completeWithError(error);
                        }
                    }
                });

            } catch (Exception e) {
                log.error("对话处理异常", e);
                sendError(emitter, "系统异常: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 获取会话列表
     */
    public Page<Conversation> getConversations(Long userId, int page, int size) {
        Page<Conversation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getStatus, 1)
                .orderByDesc(Conversation::getUpdatedAt);
        return this.page(pageParam, wrapper);
    }

    /**
     * 获取会话消息列表
     */
    public List<Message> getMessages(Long conversationId, Long userId) {
        Conversation conversation = this.getById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.CONVERSATION_NOT_FOUND);
        }
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getCreatedAt);
        return messageMapper.selectList(wrapper);
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conversation = this.getById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.CONVERSATION_NOT_FOUND);
        }
        conversation.setStatus(0);
        this.updateById(conversation);
    }

    /**
     * 消息反馈
     */
    public void feedback(Long messageId, String feedback) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 写入 feedback 表（带用户追踪，支持 toggle 和统计）
        Long userId = com.campus.common.utils.UserContext.getUserId();
        String result = feedbackService.toggle(userId, messageId, feedback, null);
        // message.feedback 保留兼容旧逻辑
        message.setFeedback(result);
        messageMapper.updateById(message);
    }

    private String buildConversationHistory(Long conversationId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByDesc(Message::getCreatedAt)
                .last("LIMIT 20");
        List<Message> messages = messageMapper.selectList(wrapper);
        if (messages.isEmpty()) {
            return "";
        }
        StringBuilder history = new StringBuilder();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            history.append(msg.getRole().equals("user") ? "用户: " : "助手: ")
                    .append(msg.getContent()).append("\n");
        }
        return history.toString();
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
