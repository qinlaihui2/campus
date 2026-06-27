package com.campus.market.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.result.PageResult;
import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import com.campus.market.dto.*;
import com.campus.market.service.MarketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    /** 获取当前用户（未登录返回 null，不影响公开浏览） */
    private Long getCurrentUserId() {
        return UserContext.getUserId();
    }

    // ==================== 商品 ====================

    @GetMapping
    public R<PageResult<MarketItemVO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = getCurrentUserId();
        Page<MarketItemVO> result = marketService.listItems(
                category, condition, keyword, minPrice, maxPrice, sort, page, size, currentUserId);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/{id}")
    public R<MarketItemVO> detail(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        MarketItemVO vo = marketService.getDetail(id, currentUserId);
        return R.ok(vo);
    }

    @PostMapping
    public R<Map<String, Object>> publish(@Valid @RequestBody PublishItemRequest request) {
        Long userId = UserContext.getUserId();
        marketService.publish(request, userId);
        return R.ok(Map.of("success", true));
    }

    @PutMapping("/{id}")
    public R<String> update(@PathVariable Long id, @Valid @RequestBody PublishItemRequest request) {
        Long userId = UserContext.getUserId();
        marketService.updateItem(id, request, userId);
        return R.ok("更新成功");
    }

    @PostMapping("/{id}/sold")
    public R<String> markAsSold(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        marketService.markAsSold(id, userId);
        return R.ok("已标记为已售");
    }

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        marketService.deleteItem(id, userId);
        return R.ok("删除成功");
    }

    @GetMapping("/my")
    public R<PageResult<MarketItemVO>> myItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        Page<MarketItemVO> result = marketService.listMyItems(userId, page, size);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    // ==================== 点赞 ====================

    @PostMapping("/{id}/like")
    public R<Map<String, Object>> toggleLike(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        boolean liked = marketService.toggleLike(id, userId);
        return R.ok(Map.of("liked", liked));
    }

    // ==================== 评论 ====================

    @GetMapping("/{id}/comments")
    public R<List<MarketCommentVO>> comments(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        List<MarketCommentVO> comments = marketService.getComments(id, currentUserId);
        return R.ok(comments);
    }

    @PostMapping("/{id}/comments")
    public R<Map<String, Object>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request) {
        Long userId = UserContext.getUserId();
        MarketCommentVO comment = marketService.addComment(id, userId,
                request.getContent(), request.getParentId(), request.getReplyToUserId());
        return R.ok(Map.of("success", true, "comment", comment));
    }

    @DeleteMapping("/comments/{commentId}")
    public R<String> deleteComment(@PathVariable Long commentId) {
        Long userId = UserContext.getUserId();
        marketService.deleteComment(commentId, userId);
        return R.ok("删除成功");
    }

    // ==================== 出价 ====================

    @PostMapping("/{id}/offer")
    public R<Map<String, Object>> makeOffer(@PathVariable Long id, @Valid @RequestBody OfferRequest request) {
        Long buyerId = UserContext.getUserId();
        marketService.makeOffer(id, buyerId, request);
        return R.ok(Map.of("success", true));
    }

    @GetMapping("/offers/received")
    public R<PageResult<OfferVO>> receivedOffers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        Page<OfferVO> result = marketService.listReceivedOffers(userId, page, size);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/offers/sent")
    public R<PageResult<OfferVO>> sentOffers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        Page<OfferVO> result = marketService.listSentOffers(userId, page, size);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @PutMapping("/offers/{id}/accept")
    public R<String> acceptOffer(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        marketService.acceptOffer(id, userId);
        return R.ok("已接受出价");
    }

    @PutMapping("/offers/{id}/reject")
    public R<String> rejectOffer(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        marketService.rejectOffer(id, userId);
        return R.ok("已拒绝出价");
    }
}
