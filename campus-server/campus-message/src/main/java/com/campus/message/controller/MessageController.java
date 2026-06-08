package com.campus.message.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.entity.User;
import com.campus.common.result.PageResult;
import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import com.campus.message.dto.ConversationVO;
import com.campus.message.entity.Message;
import com.campus.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/conversations")
    public R<PageResult<ConversationVO>> conversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        Page<ConversationVO> result = messageService.getConversations(userId, page, size);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/conversations/{id}")
    public R<PageResult<Message>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = UserContext.getUserId();
        Page<Message> result = messageService.getMessages(id, userId, page, size);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @PostMapping("/send")
    public R<Message> send(@RequestBody Map<String, Object> body) {
        Long senderId = UserContext.getUserId();
        Long toUserId = Long.valueOf(body.get("toUserId").toString());
        String content = body.get("content").toString();
        String type = body.getOrDefault("type", "TEXT").toString();
        Message msg = messageService.sendMessage(senderId, toUserId, content, type);
        return R.ok(msg);
    }

    @DeleteMapping("/conversations/{id}")
    public R<String> deleteConversation(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        messageService.deleteConversation(id, userId);
        return R.ok("已删除");
    }

    @GetMapping("/unread-count")
    public R<Integer> unreadCount() {
        Long userId = UserContext.getUserId();
        return R.ok(messageService.getUnreadCount(userId));
    }

    @GetMapping("/search-user")
    public R<List<User>> searchUser(@RequestParam String keyword) {
        Long userId = UserContext.getUserId();
        return R.ok(messageService.searchUser(keyword, userId));
    }
}
