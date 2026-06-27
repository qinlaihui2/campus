package com.campus.square.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.result.PageResult;
import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import com.campus.square.dto.CommentVO;
import com.campus.square.dto.PublishRequest;
import com.campus.square.dto.SquarePostVO;
import com.campus.square.service.SquarePostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/square")
@RequiredArgsConstructor
public class SquareController {

    private final SquarePostService squarePostService;

    @GetMapping
    public R<PageResult<SquarePostVO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = getCurrentUserId();
        Page<SquarePostVO> result = squarePostService.listPosts(category, keyword, page, size, currentUserId);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/hot")
    public R<PageResult<SquarePostVO>> hot(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = getCurrentUserId();
        Page<SquarePostVO> result = squarePostService.listHot(page, size, currentUserId);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/{id}")
    public R<SquarePostVO> detail(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        return R.ok(squarePostService.getDetail(id, currentUserId));
    }

    @PostMapping("/publish")
    public R<Map<String, Object>> publish(@Valid @RequestBody PublishRequest request) {
        Long userId = UserContext.getUserId();
        squarePostService.publish(request, userId);
        return R.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/like")
    public R<Map<String, Object>> like(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        boolean liked = squarePostService.toggleLike(id, userId);
        return R.ok(Map.of("liked", liked));
    }

    @PostMapping("/{id}/favorite")
    public R<Map<String, Object>> favorite(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        boolean favorited = squarePostService.toggleFavorite(id, userId);
        return R.ok(Map.of("favorited", favorited));
    }

    @GetMapping("/my")
    public R<PageResult<SquarePostVO>> myPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        Page<SquarePostVO> result = squarePostService.listMyPosts(page, size, userId);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    // ========== 评论 ==========

    @GetMapping("/{id}/comments")
    public R<List<CommentVO>> comments(@PathVariable Long id,
                                       @RequestParam(defaultValue = "hot") String sort) {
        Long currentUserId = getCurrentUserId();
        return R.ok(squarePostService.getComments(id, sort, currentUserId));
    }

    @PostMapping("/{id}/comments")
    public R<CommentVO> addComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = UserContext.getUserId();
        String content = body.get("content");
        Long parentId = body.containsKey("parentId") && !body.get("parentId").isEmpty()
                ? Long.parseLong(body.get("parentId")) : null;
        Long replyToUserId = body.containsKey("replyToUserId") && !body.get("replyToUserId").isEmpty()
                ? Long.parseLong(body.get("replyToUserId")) : null;
        return R.ok(squarePostService.addComment(id, userId, content, parentId, replyToUserId));
    }

    @PostMapping("/comments/{commentId}/like")
    public R<Map<String, Object>> likeComment(@PathVariable Long commentId) {
        Long userId = UserContext.getUserId();
        boolean liked = squarePostService.toggleCommentLike(commentId, userId);
        return R.ok(Map.of("liked", liked));
    }

    @DeleteMapping("/comments/{commentId}")
    public R<String> deleteComment(@PathVariable Long commentId) {
        Long userId = UserContext.getUserId();
        squarePostService.deleteComment(commentId, userId);
        return R.ok("删除成功");
    }

    private Long getCurrentUserId() {
        try {
            return UserContext.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
