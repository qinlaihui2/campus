package com.campus.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.entity.User;
import com.campus.common.exception.BusinessException;
import com.campus.common.mapper.UserMapper;
import com.campus.common.result.ResultCode;
import com.campus.message.dto.ConversationVO;
import com.campus.message.entity.Conversation;
import com.campus.message.entity.Message;
import com.campus.message.mapper.ImConversationMapper;
import com.campus.message.mapper.ImMessageMapper;
import com.campus.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<ImConversationMapper, Conversation> implements MessageService {

    private final ImMessageMapper messageMapper;
    private final UserMapper userMapper;

    @Override
    public Page<ConversationVO> getConversations(Long userId, int page, int size) {
        Page<Conversation> pageParam = new Page<>(page, size);
        Page<Conversation> result = (Page<Conversation>) baseMapper.selectByUser(pageParam, userId);

        // Fetch peer user info
        List<Long> peerIds = result.getRecords().stream()
                .map(c -> c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id())
                .collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(peerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Page<ConversationVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(c -> {
            ConversationVO vo = new ConversationVO();
            org.springframework.beans.BeanUtils.copyProperties(c, vo);
            Long peerId = c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id();
            User peer = userMap.get(peerId);
            vo.setPeerName(peer != null ? (peer.getNickname() != null ? peer.getNickname() : peer.getUsername()) : "用户" + peerId);
            vo.setPeerAvatar(peer != null ? peer.getAvatar() : null);
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<Message> getMessages(Long conversationId, Long userId, int page, int size) {
        Conversation conv = getConversationById(conversationId, userId);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 标记对方发来的消息为已读
        LambdaQueryWrapper<Message> unreadWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .ne(Message::getSenderId, userId)
                .eq(Message::getIsRead, 0);
        List<Message> unreadMessages = messageMapper.selectList(unreadWrapper);
        for (Message msg : unreadMessages) {
            msg.setIsRead(1);
            msg.setReadAt(LocalDateTime.now());
            messageMapper.updateById(msg);
        }

        Page<Message> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByDesc(Message::getCreatedAt);
        return messageMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional
    public Message sendMessage(Long senderId, Long toUserId, String content, String type) {
        if (senderId.equals(toUserId)) {
            throw new BusinessException(400, "不能给自己发消息");
        }
        Conversation conv = findOrCreateConversation(senderId, toUserId);
        Message message = new Message();
        message.setConversationId(conv.getId());
        message.setSenderId(senderId);
        message.setContent(content);
        message.setType(type != null ? type : "TEXT");
        message.setIsRead(0);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);

        // 更新会话最后消息
        conv.setLastMessage(content.length() > 50 ? content.substring(0, 50) : content);
        conv.setLastMessageAt(message.getCreatedAt());
        this.updateById(conv);

        return message;
    }

    @Override
    @Transactional
    public Conversation findOrCreateConversation(Long userId1, Long userId2) {
        Long smaller = Math.min(userId1, userId2);
        Long larger = Math.max(userId1, userId2);

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUser1Id, smaller)
                .eq(Conversation::getUser2Id, larger);
        Conversation conv = this.getOne(wrapper);

        if (conv == null) {
            conv = new Conversation();
            conv.setUser1Id(smaller);
            conv.setUser2Id(larger);
            conv.setUser1Deleted(0);
            conv.setUser2Deleted(0);
            conv.setCreatedAt(LocalDateTime.now());
            this.save(conv);
        } else {
            // 恢复删除状态
            if (conv.getUser1Id().equals(userId1) && conv.getUser1Deleted() == 1) {
                conv.setUser1Deleted(0);
            }
            if (conv.getUser2Id().equals(userId1) && conv.getUser2Deleted() == 1) {
                conv.setUser2Deleted(0);
            }
            this.updateById(conv);
        }
        return conv;
    }

    @Override
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conv = this.getById(conversationId);
        if (conv == null) return;
        if (conv.getUser1Id().equals(userId)) {
            conv.setUser1Deleted(1);
        } else if (conv.getUser2Id().equals(userId)) {
            conv.setUser2Deleted(1);
        }
        this.updateById(conv);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return messageMapper.countUnread(userId);
    }

    @Override
    public List<User> searchUser(String keyword, Long excludeUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword))
                .ne(User::getId, excludeUserId)
                .last("LIMIT 20");
        return userMapper.selectList(wrapper);
    }

    @Override
    public Conversation getConversationById(Long conversationId, Long userId) {
        Conversation conv = this.getById(conversationId);
        if (conv == null) return null;
        if (conv.getUser1Id().equals(userId) && conv.getUser1Deleted() == 1) return null;
        if (conv.getUser2Id().equals(userId) && conv.getUser2Deleted() == 1) return null;
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return conv;
    }
}
