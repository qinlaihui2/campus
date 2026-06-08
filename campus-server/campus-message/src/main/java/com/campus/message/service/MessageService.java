package com.campus.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.entity.User;
import com.campus.message.dto.ConversationVO;
import com.campus.message.entity.Conversation;
import com.campus.message.entity.Message;

import java.util.List;

public interface MessageService {

    Page<ConversationVO> getConversations(Long userId, int page, int size);

    Page<Message> getMessages(Long conversationId, Long userId, int page, int size);

    Message sendMessage(Long senderId, Long toUserId, String content, String type);

    Conversation findOrCreateConversation(Long userId1, Long userId2);

    void deleteConversation(Long conversationId, Long userId);

    int getUnreadCount(Long userId);

    List<User> searchUser(String keyword, Long excludeUserId);

    Conversation getConversationById(Long conversationId, Long userId);
}
