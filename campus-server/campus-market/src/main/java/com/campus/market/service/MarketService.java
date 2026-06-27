package com.campus.market.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.dto.*;
import com.campus.market.entity.MarketItem;

import java.util.List;

public interface MarketService {

    // 商品
    Page<MarketItemVO> listItems(String category, String condition, String keyword,
                                 Double minPrice, Double maxPrice,
                                 String sort, int page, int size, Long currentUserId);

    MarketItemVO getDetail(Long id, Long currentUserId);

    void publish(PublishItemRequest request, Long userId);

    void updateItem(Long itemId, PublishItemRequest request, Long userId);

    void markAsSold(Long itemId, Long userId);

    void deleteItem(Long itemId, Long userId);

    Page<MarketItemVO> listMyItems(Long userId, int page, int size);

    // 点赞
    boolean toggleLike(Long itemId, Long userId);

    // 评论
    List<MarketCommentVO> getComments(Long itemId, Long currentUserId);

    MarketCommentVO addComment(Long itemId, Long userId, String content, Long parentId, Long replyToUserId);

    void deleteComment(Long commentId, Long userId);

    // 出价
    void makeOffer(Long itemId, Long buyerId, OfferRequest request);

    Page<OfferVO> listReceivedOffers(Long sellerId, int page, int size);

    Page<OfferVO> listSentOffers(Long buyerId, int page, int size);

    void acceptOffer(Long offerId, Long userId);

    void rejectOffer(Long offerId, Long userId);
}
