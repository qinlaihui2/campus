package com.campus.market.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.entity.User;
import com.campus.common.exception.BusinessException;
import com.campus.common.mapper.UserMapper;
import com.campus.common.notification.NotificationEvent;
import com.campus.common.notification.NotificationHelper;
import com.campus.common.result.ResultCode;
import com.campus.market.dto.*;
import com.campus.market.entity.*;
import com.campus.market.mapper.*;
import com.campus.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketServiceImpl extends ServiceImpl<MarketItemMapper, MarketItem>
        implements MarketService {

    private final MarketLikeMapper marketLikeMapper;
    private final MarketCommentMapper marketCommentMapper;
    private final MarketOfferMapper marketOfferMapper;
    private final UserMapper userMapper;
    private final NotificationHelper notificationHelper;

    // ==================== 商品 ====================

    @Override
    public Page<MarketItemVO> listItems(String category, String condition, String keyword,
                                        Double minPrice, Double maxPrice,
                                        String sort, int page, int size, Long currentUserId) {
        LambdaQueryWrapper<MarketItem> wrapper = new LambdaQueryWrapper<MarketItem>()
                .eq(MarketItem::getStatus, "ON_SALE");

        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(MarketItem::getCategory, category);
        }
        if (StrUtil.isNotBlank(condition)) {
            wrapper.eq(MarketItem::getCondition, condition);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(MarketItem::getTitle, keyword)
                    .or().like(MarketItem::getDescription, keyword));
        }
        if (minPrice != null) {
            wrapper.ge(MarketItem::getPrice, BigDecimal.valueOf(minPrice));
        }
        if (maxPrice != null) {
            wrapper.le(MarketItem::getPrice, BigDecimal.valueOf(maxPrice));
        }

        // 排序
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(MarketItem::getPrice);
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc(MarketItem::getPrice);
        } else {
            // newest (default)
            wrapper.orderByDesc(MarketItem::getCreatedAt);
        }

        Page<MarketItem> pageParam = new Page<>(page, size);
        Page<MarketItem> result = this.page(pageParam, wrapper);
        return convertToVO(result, currentUserId);
    }

    @Override
    public MarketItemVO getDetail(Long id, Long currentUserId) {
        MarketItem item = this.getById(id);
        if (item == null || "REMOVED".equals(item.getStatus())) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }

        // 原子增加浏览次数
        LambdaUpdateWrapper<MarketItem> updateWrapper = new LambdaUpdateWrapper<MarketItem>()
                .eq(MarketItem::getId, id)
                .setSql("view_count = view_count + 1");
        this.update(updateWrapper);
        item.setViewCount(item.getViewCount() + 1);

        Map<Long, User> userMap = batchLoadUsers(Set.of(item.getUserId()));
        MarketItemVO vo = toVO(item, currentUserId, userMap);
        // 解析图片 JSON 数组（详情页显示全部图片）
        if (StrUtil.isNotBlank(item.getImages())) {
            vo.setImageUrls(JSONUtil.parseArray(item.getImages()).toList(String.class));
        }
        return vo;
    }

    @Override
    @Transactional
    public void publish(PublishItemRequest request, Long userId) {
        MarketItem item = new MarketItem();
        item.setUserId(userId);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setOriginalPrice(request.getOriginalPrice());
        item.setCategory(request.getCategory());
        item.setCondition(request.getCondition());
        item.setImages(request.getImages() != null ? JSONUtil.toJsonStr(request.getImages()) : null);
        item.setStatus("ON_SALE");
        item.setViewCount(0);
        item.setLikeCount(0);
        this.save(item);
    }

    @Override
    @Transactional
    public void updateItem(Long itemId, PublishItemRequest request, Long userId) {
        MarketItem item = this.getById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ITEM_NOT_YOURS);
        }
        if (!"ON_SALE".equals(item.getStatus())) {
            throw new BusinessException(ResultCode.ITEM_SOLD);
        }

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setOriginalPrice(request.getOriginalPrice());
        item.setCategory(request.getCategory());
        item.setCondition(request.getCondition());
        item.setImages(request.getImages() != null ? JSONUtil.toJsonStr(request.getImages()) : null);
        this.updateById(item);
    }

    @Override
    @Transactional
    public void markAsSold(Long itemId, Long userId) {
        MarketItem item = this.getById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ITEM_NOT_YOURS);
        }
        item.setStatus("SOLD");
        this.updateById(item);
    }

    @Override
    @Transactional
    public void deleteItem(Long itemId, Long userId) {
        MarketItem item = this.getById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ITEM_NOT_YOURS);
        }
        this.removeById(itemId);
    }

    @Override
    public Page<MarketItemVO> listMyItems(Long userId, int page, int size) {
        LambdaQueryWrapper<MarketItem> wrapper = new LambdaQueryWrapper<MarketItem>()
                .eq(MarketItem::getUserId, userId)
                .orderByDesc(MarketItem::getCreatedAt);
        Page<MarketItem> pageParam = new Page<>(page, size);
        Page<MarketItem> result = this.page(pageParam, wrapper);
        return convertToVO(result, userId);
    }

    // ==================== 点赞 ====================

    @Override
    @Transactional
    public boolean toggleLike(Long itemId, Long userId) {
        MarketItem item = this.getById(itemId);
        if (item == null || "REMOVED".equals(item.getStatus())) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }

        LambdaQueryWrapper<MarketLike> wrapper = new LambdaQueryWrapper<MarketLike>()
                .eq(MarketLike::getItemId, itemId)
                .eq(MarketLike::getUserId, userId);
        MarketLike existing = marketLikeMapper.selectOne(wrapper);

        if (existing != null) {
            marketLikeMapper.deleteById(existing.getId());
            item.setLikeCount(Math.max(0, item.getLikeCount() - 1));
            this.updateById(item);
            return false;
        } else {
            MarketLike like = new MarketLike();
            like.setItemId(itemId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            marketLikeMapper.insert(like);
            item.setLikeCount(item.getLikeCount() + 1);
            this.updateById(item);

            // 通知商品主人
            if (!item.getUserId().equals(userId)) {
                notificationHelper.send(NotificationEvent.builder()
                        .type("LIKE")
                        .userId(item.getUserId())
                        .title("有人赞了你的商品")
                        .content("你的商品《" + item.getTitle() + "》收到一个新赞")
                        .targetType("MARKET")
                        .targetId(itemId)
                        .build());
            }

            return true;
        }
    }

    // ==================== 评论 ====================

    @Override
    public List<MarketCommentVO> getComments(Long itemId, Long currentUserId) {
        LambdaQueryWrapper<MarketComment> wrapper = new LambdaQueryWrapper<MarketComment>()
                .eq(MarketComment::getItemId, itemId)
                .eq(MarketComment::getDeleted, 0)
                .orderByDesc(MarketComment::getCreatedAt);

        List<MarketComment> all = marketCommentMapper.selectList(wrapper);
        return buildCommentTree(all, currentUserId);
    }

    @Override
    @Transactional
    public MarketCommentVO addComment(Long itemId, Long userId, String content,
                                       Long parentId, Long replyToUserId) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        MarketItem item = this.getById(itemId);
        if (item == null || "REMOVED".equals(item.getStatus())) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }

        MarketComment comment = new MarketComment();
        comment.setItemId(itemId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setLikeCount(0);
        comment.setDeleted(0);
        comment.setCreatedAt(LocalDateTime.now());
        marketCommentMapper.insert(comment);

        // 通知商品主人
        if (!item.getUserId().equals(userId)) {
            String shortContent = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            notificationHelper.send(NotificationEvent.builder()
                    .type(parentId != null ? "REPLY" : "COMMENT")
                    .userId(item.getUserId())
                    .title(parentId != null ? "有人回复了你的评论" : "有人评论了你的商品")
                    .content(shortContent)
                    .targetType("MARKET")
                    .targetId(itemId)
                    .build());
        }

        // 如果是回复，通知被回复的人（如果既不是自己也不是商品主人）
        if (replyToUserId != null && !replyToUserId.equals(userId) && !replyToUserId.equals(item.getUserId())) {
            String shortContent = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            notificationHelper.send(NotificationEvent.builder()
                    .type("REPLY")
                    .userId(replyToUserId)
                    .title("有人回复了你的评论")
                    .content(shortContent)
                    .targetType("MARKET")
                    .targetId(itemId)
                    .build());
        }

        Set<Long> ids = new HashSet<>();
        ids.add(userId);
        if (replyToUserId != null) ids.add(replyToUserId);
        return toCommentVO(comment, userId, batchLoadUsers(ids));
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        MarketComment comment = marketCommentMapper.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        comment.setDeleted(1);
        marketCommentMapper.updateById(comment);
    }

    // ==================== 出价 ====================

    @Override
    @Transactional
    public void makeOffer(Long itemId, Long buyerId, OfferRequest request) {
        MarketItem item = this.getById(itemId);
        if (item == null || "REMOVED".equals(item.getStatus())) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }
        if (!"ON_SALE".equals(item.getStatus())) {
            throw new BusinessException(ResultCode.ITEM_SOLD);
        }
        if (item.getUserId().equals(buyerId)) {
            throw new BusinessException(ResultCode.CANNOT_OFFER_OWN_ITEM);
        }

        MarketOffer offer = new MarketOffer();
        offer.setItemId(itemId);
        offer.setBuyerId(buyerId);
        offer.setSellerId(item.getUserId());
        offer.setPrice(request.getPrice());
        offer.setMessage(request.getMessage());
        offer.setStatus("PENDING");
        marketOfferMapper.insert(offer);

        // 通知卖家
        notificationHelper.send(NotificationEvent.builder()
                .type("OFFER")
                .userId(item.getUserId())
                .title("有人给你的商品出价了")
                .content("你的商品《" + item.getTitle() + "》收到一个新出价：¥" + request.getPrice())
                .targetType("MARKET")
                .targetId(itemId)
                .build());
    }

    @Override
    public Page<OfferVO> listReceivedOffers(Long sellerId, int page, int size) {
        Page<MarketOffer> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MarketOffer> wrapper = new LambdaQueryWrapper<MarketOffer>()
                .eq(MarketOffer::getSellerId, sellerId)
                .orderByDesc(MarketOffer::getCreatedAt);
        Page<MarketOffer> result = marketOfferMapper.selectPage(pageParam, wrapper);
        return convertOfferPage(result);
    }

    @Override
    public Page<OfferVO> listSentOffers(Long buyerId, int page, int size) {
        Page<MarketOffer> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MarketOffer> wrapper = new LambdaQueryWrapper<MarketOffer>()
                .eq(MarketOffer::getBuyerId, buyerId)
                .orderByDesc(MarketOffer::getCreatedAt);
        Page<MarketOffer> result = marketOfferMapper.selectPage(pageParam, wrapper);
        return convertOfferPage(result);
    }

    @Override
    @Transactional
    public void acceptOffer(Long offerId, Long userId) {
        MarketOffer offer = marketOfferMapper.selectById(offerId);
        if (offer == null) {
            throw new BusinessException(ResultCode.OFFER_NOT_FOUND);
        }
        if (!"PENDING".equals(offer.getStatus())) {
            throw new BusinessException(ResultCode.OFFER_NOT_PENDING);
        }

        MarketItem item = this.getById(offer.getItemId());
        if (item == null) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ITEM_NOT_YOURS);
        }
        if (!"ON_SALE".equals(item.getStatus())) {
            throw new BusinessException(ResultCode.ITEM_SOLD);
        }

        // 接受此出价
        offer.setStatus("ACCEPTED");
        marketOfferMapper.updateById(offer);

        // 商品标记为已售
        item.setStatus("SOLD");
        this.updateById(item);

        // 拒绝该商品其他待处理的出价
        LambdaUpdateWrapper<MarketOffer> rejectWrapper = new LambdaUpdateWrapper<MarketOffer>()
                .eq(MarketOffer::getItemId, item.getId())
                .eq(MarketOffer::getStatus, "PENDING")
                .ne(MarketOffer::getId, offerId)
                .set(MarketOffer::getStatus, "CANCELLED");
        MarketOffer cancelledOffer = new MarketOffer();
        cancelledOffer.setStatus("CANCELLED");
        marketOfferMapper.update(cancelledOffer, rejectWrapper);

        // 通知买家
        notificationHelper.send(NotificationEvent.builder()
                .type("SYSTEM")
                .userId(offer.getBuyerId())
                .title("你的出价被接受了")
                .content("你对《" + item.getTitle() + "》的出价 ¥" + offer.getPrice() + " 已被卖家接受")
                .targetType("MARKET")
                .targetId(item.getId())
                .build());
    }

    @Override
    @Transactional
    public void rejectOffer(Long offerId, Long userId) {
        MarketOffer offer = marketOfferMapper.selectById(offerId);
        if (offer == null) {
            throw new BusinessException(ResultCode.OFFER_NOT_FOUND);
        }
        if (!"PENDING".equals(offer.getStatus())) {
            throw new BusinessException(ResultCode.OFFER_NOT_PENDING);
        }

        MarketItem item = this.getById(offer.getItemId());
        if (item == null) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ITEM_NOT_YOURS);
        }

        offer.setStatus("REJECTED");
        marketOfferMapper.updateById(offer);
    }

    // ==================== 私有方法 ====================

    /** 批量加载用户信息，避免 N+1 */
    private Map<Long, User> batchLoadUsers(Set<Long> userIds) {
        // Make mutable copy since source might be immutable (e.g. Set.of)
        Set<Long> ids = new HashSet<>(userIds);
        ids.remove(null);
        if (ids.isEmpty()) return Collections.emptyMap();
        List<User> users = userMapper.selectBatchIds(ids);
        return users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private Page<MarketItemVO> convertToVO(Page<MarketItem> page, Long currentUserId) {
        // 批量加载用户
        Set<Long> userIds = page.getRecords().stream()
                .map(MarketItem::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = batchLoadUsers(userIds);

        Page<MarketItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(item -> toVO(item, currentUserId, userMap))
                .collect(Collectors.toList()));
        return voPage;
    }

    private MarketItemVO toVO(MarketItem item, Long currentUserId, Map<Long, User> userMap) {
        MarketItemVO vo = new MarketItemVO();
        vo.setId(item.getId());
        vo.setUserId(item.getUserId());
        vo.setTitle(item.getTitle());
        vo.setDescription(item.getDescription());
        vo.setPrice(item.getPrice());
        vo.setOriginalPrice(item.getOriginalPrice());
        vo.setCategory(item.getCategory());
        vo.setCondition(item.getCondition());
        vo.setImages(item.getImages());
        vo.setStatus(item.getStatus());
        vo.setViewCount(item.getViewCount());
        vo.setLikeCount(item.getLikeCount());
        vo.setCreatedAt(item.getCreatedAt());
        vo.setUpdatedAt(item.getUpdatedAt());

        // 列表页只取第一张图片
        if (StrUtil.isNotBlank(item.getImages())) {
            List<String> urls = JSONUtil.parseArray(item.getImages()).toList(String.class);
            if (!urls.isEmpty()) {
                vo.setImageUrls(urls.subList(0, 1));
            }
        }

        // 用户信息
        User user = userMap.get(item.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname() != null ? user.getNickname() : "同学");
            vo.setUserAvatar(user.getAvatar());
        }

        // 是否已点赞
        if (currentUserId != null) {
            LambdaQueryWrapper<MarketLike> likeWrapper = new LambdaQueryWrapper<MarketLike>()
                    .eq(MarketLike::getItemId, item.getId())
                    .eq(MarketLike::getUserId, currentUserId);
            vo.setLiked(marketLikeMapper.selectCount(likeWrapper) > 0);
        }

        return vo;
    }

    // ==================== 评论树 ====================

    private List<MarketCommentVO> buildCommentTree(List<MarketComment> all, Long currentUserId) {
        // 批量加载评论用户
        Set<Long> userIds = new HashSet<>();
        for (MarketComment c : all) {
            userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) userIds.add(c.getReplyToUserId());
        }
        Map<Long, User> userMap = batchLoadUsers(userIds);

        return all.stream()
                .filter(c -> c.getParentId() == null)
                .map(root -> buildCommentNode(root, all, currentUserId, userMap))
                .collect(Collectors.toList());
    }

    private MarketCommentVO buildCommentNode(MarketComment comment, List<MarketComment> all,
                                              Long currentUserId, Map<Long, User> userMap) {
        MarketCommentVO vo = toCommentVO(comment, currentUserId, userMap);
        List<MarketCommentVO> children = all.stream()
                .filter(c -> comment.getId().equals(c.getParentId()))
                .map(child -> buildCommentNode(child, all, currentUserId, userMap))
                .collect(Collectors.toList());
        vo.setReplies(children.isEmpty() ? null : children);
        return vo;
    }

    private MarketCommentVO toCommentVO(MarketComment comment, Long currentUserId, Map<Long, User> userMap) {
        MarketCommentVO vo = new MarketCommentVO();
        vo.setId(comment.getId());
        vo.setItemId(comment.getItemId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setParentId(comment.getParentId());
        vo.setReplyToUserId(comment.getReplyToUserId());

        User user = userMap.get(comment.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname() != null ? user.getNickname() : "同学");
            vo.setUserAvatar(user.getAvatar());
        }
        if (comment.getReplyToUserId() != null) {
            User replyTo = userMap.get(comment.getReplyToUserId());
            if (replyTo != null) {
                vo.setReplyToUserNickname(replyTo.getNickname() != null ? replyTo.getNickname() : "同学");
            }
        }

        return vo;
    }

    // ==================== 出价 VO ====================

    private Page<OfferVO> convertOfferPage(Page<MarketOffer> page) {
        // 批量加载用户 + 商品标题
        Set<Long> userIds = new HashSet<>();
        Set<Long> itemIds = new HashSet<>();
        for (MarketOffer offer : page.getRecords()) {
            userIds.add(offer.getBuyerId());
            userIds.add(offer.getSellerId());
            itemIds.add(offer.getItemId());
        }
        Map<Long, User> userMap = batchLoadUsers(userIds);
        Map<Long, String> titleMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            List<MarketItem> items = this.listByIds(itemIds);
            for (MarketItem item : items) {
                titleMap.put(item.getId(), item.getTitle());
            }
        }

        Page<OfferVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        final Map<Long, String> finalTitleMap = titleMap;
        voPage.setRecords(page.getRecords().stream()
                .map(offer -> toOfferVO(offer, userMap, finalTitleMap))
                .collect(Collectors.toList()));
        return voPage;
    }

    private OfferVO toOfferVO(MarketOffer offer, Map<Long, User> userMap, Map<Long, String> titleMap) {
        OfferVO vo = new OfferVO();
        vo.setId(offer.getId());
        vo.setItemId(offer.getItemId());
        vo.setBuyerId(offer.getBuyerId());
        vo.setSellerId(offer.getSellerId());
        vo.setPrice(offer.getPrice());
        vo.setMessage(offer.getMessage());
        vo.setStatus(offer.getStatus());
        vo.setCreatedAt(offer.getCreatedAt());
        vo.setUpdatedAt(offer.getUpdatedAt());

        vo.setItemTitle(titleMap.getOrDefault(offer.getItemId(), ""));

        User buyer = userMap.get(offer.getBuyerId());
        if (buyer != null) {
            vo.setBuyerName(buyer.getNickname() != null ? buyer.getNickname() : buyer.getUsername());
            vo.setBuyerAvatar(buyer.getAvatar());
        }

        User seller = userMap.get(offer.getSellerId());
        if (seller != null) {
            vo.setSellerName(seller.getNickname() != null ? seller.getNickname() : seller.getUsername());
            vo.setSellerAvatar(seller.getAvatar());
        }

        return vo;
    }
}
