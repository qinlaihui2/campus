package com.campus.course.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.result.PageResult;
import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import com.campus.course.dto.CourseDetailVO;
import com.campus.course.dto.CourseVO;
import com.campus.course.dto.CommentVO;
import com.campus.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public R<PageResult<CourseVO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long uid = getCurrentUserId();
        Page<CourseVO> result = courseService.listCourses(category, page, size, uid);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/{id}")
    public R<CourseDetailVO> detail(@PathVariable Long id) {
        Long uid = getCurrentUserId();
        return R.ok(courseService.getDetail(id, uid));
    }

    @PostMapping("/{id}/like")
    public R<Map<String, Object>> like(@PathVariable Long id) {
        Long uid = UserContext.getUserId();
        boolean liked = courseService.toggleLike(id, uid);
        return R.ok(Map.of("liked", liked));
    }

    @PostMapping("/{id}/favorite")
    public R<Map<String, Object>> favorite(@PathVariable Long id) {
        Long uid = UserContext.getUserId();
        boolean favorited = courseService.toggleFavorite(id, uid);
        return R.ok(Map.of("favorited", favorited));
    }

    @GetMapping("/favorites")
    public R<PageResult<CourseVO>> favorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long uid = UserContext.getUserId();
        Page<CourseVO> result = courseService.listFavorites(page, size, uid);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    // 评论
    @GetMapping("/{id}/comments")
    public R<List<CommentVO>> comments(@PathVariable Long id,
                                       @RequestParam(defaultValue = "hot") String sort) {
        Long uid = getCurrentUserId();
        return R.ok(courseService.getComments(id, sort, uid));
    }

    @PostMapping("/{id}/comments")
    public R<CommentVO> addComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long uid = UserContext.getUserId();
        String content = body.get("content");
        Long parentId = body.containsKey("parentId") && !body.get("parentId").isEmpty()
                ? Long.parseLong(body.get("parentId")) : null;
        Long replyToUserId = body.containsKey("replyToUserId") && !body.get("replyToUserId").isEmpty()
                ? Long.parseLong(body.get("replyToUserId")) : null;
        return R.ok(courseService.addComment(id, uid, content, parentId, replyToUserId));
    }

    @PostMapping("/comments/{commentId}/like")
    public R<Map<String, Object>> likeComment(@PathVariable Long commentId) {
        Long uid = UserContext.getUserId();
        boolean liked = courseService.toggleCommentLike(commentId, uid);
        return R.ok(Map.of("liked", liked));
    }

    @DeleteMapping("/comments/{commentId}")
    public R<String> deleteComment(@PathVariable Long commentId) {
        Long uid = UserContext.getUserId();
        courseService.deleteComment(commentId, uid);
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
