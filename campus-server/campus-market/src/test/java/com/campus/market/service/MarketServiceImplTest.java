package com.campus.market.service;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.entity.User;
import com.campus.common.exception.BusinessException;
import com.campus.common.mapper.UserMapper;
import com.campus.common.notification.NotificationHelper;
import com.campus.market.dto.*;
import com.campus.market.entity.*;
import com.campus.market.mapper.*;
import com.campus.market.service.impl.MarketServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketService 单元测试")
class MarketServiceImplTest {

    @Mock private MarketItemMapper marketItemMapper;
    @Mock private MarketLikeMapper marketLikeMapper;
    @Mock private MarketCommentMapper marketCommentMapper;
    @Mock private MarketOfferMapper marketOfferMapper;
    @Mock private UserMapper userMapper;
    @Mock private NotificationHelper notificationHelper;

    @InjectMocks
    private MarketServiceImpl marketService;

    private MarketItem sampleItem;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        // Inject baseMapper into the parent ServiceImpl (required by MyBatis-Plus)
        ReflectionTestUtils.setField(marketService, "baseMapper", marketItemMapper);

        sampleItem = new MarketItem();
        sampleItem.setId(1L);
        sampleItem.setUserId(100L);
        sampleItem.setTitle("测试商品");
        sampleItem.setDescription("描述");
        sampleItem.setPrice(new BigDecimal("99.00"));
        sampleItem.setCategory("电子数码");
        sampleItem.setCondition("NEW");
        sampleItem.setStatus("ON_SALE");
        sampleItem.setViewCount(10);
        sampleItem.setLikeCount(5);

        sampleUser = new User();
        sampleUser.setId(100L);
        sampleUser.setNickname("测试用户");
        sampleUser.setAvatar("avatar.jpg");
    }

    // ==================== listItems ====================

    @Nested
    @DisplayName("商品列表查询")
    class ListItems {

        @Test
        @DisplayName("分页返回在售商品")
        void shouldReturnPageOfOnSaleItems() {
            Page<MarketItem> page = new Page<>(1, 10);
            page.setRecords(List.of(sampleItem));
            page.setTotal(1);
            when(marketItemMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
            when(userMapper.selectBatchIds(anySet())).thenReturn(List.of(sampleUser));

            Page<MarketItemVO> result = marketService.listItems(
                    null, null, null, null, null, "newest", 1, 10, null);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getTitle()).isEqualTo("测试商品");
            assertThat(result.getRecords().get(0).getUserNickname()).isEqualTo("测试用户");
        }

        @Test
        @DisplayName("支持分类过滤")
        void shouldFilterByCategory() {
            when(marketItemMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(new Page<>(1, 10));

            marketService.listItems("书籍教材", null, null, null, null, "newest", 1, 10, null);

            verify(marketItemMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }
    }

    // ==================== getDetail ====================

    @Nested
    @DisplayName("商品详情")
    class GetDetail {

        @Test
        @DisplayName("返回完整详情并增加浏览量")
        void shouldReturnDetailAndIncrementView() {
            when(marketItemMapper.selectById(1L)).thenReturn(sampleItem);
            when(marketItemMapper.update(any(), any())).thenReturn(1);
            when(userMapper.selectBatchIds(anySet())).thenReturn(List.of(sampleUser));

            MarketItemVO vo = marketService.getDetail(1L, null);

            assertThat(vo.getTitle()).isEqualTo("测试商品");
            assertThat(vo.getViewCount()).isEqualTo(11); // incremented
        }

        @Test
        @DisplayName("商品不存在时抛异常")
        void shouldThrowWhenNotFound() {
            when(marketItemMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> marketService.getDetail(999L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("商品不存在");
        }

        @Test
        @DisplayName("已删除商品抛异常")
        void shouldThrowWhenRemoved() {
            sampleItem.setStatus("REMOVED");
            when(marketItemMapper.selectById(1L)).thenReturn(sampleItem);

            assertThatThrownBy(() -> marketService.getDetail(1L, null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ==================== publish ====================

    @Nested
    @DisplayName("发布商品")
    class Publish {

        @Test
        @DisplayName("成功发布商品")
        void shouldPublishSuccessfully() {
            PublishItemRequest req = new PublishItemRequest();
            req.setTitle("新商品");
            req.setDescription("描述");
            req.setPrice(new BigDecimal("50.00"));
            req.setCategory("书籍教材");
            req.setCondition("NEW");

            when(marketItemMapper.insert(any(MarketItem.class))).thenReturn(1);

            assertThatCode(() -> marketService.publish(req, 100L))
                    .doesNotThrowAnyException();
            verify(marketItemMapper).insert(any(MarketItem.class));
        }
    }

    // ==================== toggleLike ====================

    @Nested
    @DisplayName("点赞切换")
    class ToggleLike {

        @Test
        @DisplayName("新增点赞")
        void shouldAddLike() {
            when(marketItemMapper.selectById(1L)).thenReturn(sampleItem);
            when(marketLikeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(marketLikeMapper.insert(any(MarketLike.class))).thenReturn(1);
            when(marketItemMapper.updateById(any(MarketItem.class))).thenReturn(1);

            boolean liked = marketService.toggleLike(1L, 200L);

            assertThat(liked).isTrue();
            verify(marketLikeMapper).insert(any(MarketLike.class));
        }

        @Test
        @DisplayName("取消点赞")
        void shouldRemoveLike() {
            MarketLike existing = new MarketLike();
            existing.setId(1L);
            existing.setItemId(1L);
            existing.setUserId(200L);

            when(marketItemMapper.selectById(1L)).thenReturn(sampleItem);
            when(marketLikeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
            when(marketLikeMapper.deleteById(1L)).thenReturn(1);
            when(marketItemMapper.updateById(any(MarketItem.class))).thenReturn(1);

            boolean liked = marketService.toggleLike(1L, 200L);

            assertThat(liked).isFalse();
        }

        @Test
        @DisplayName("商品不存在时抛异常")
        void shouldThrowWhenItemNotFound() {
            when(marketItemMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> marketService.toggleLike(999L, 200L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ==================== comments ====================

    @Nested
    @DisplayName("评论功能")
    class Comments {

        @Test
        @DisplayName("返回评论树")
        void shouldReturnCommentTree() {
            MarketComment root = new MarketComment();
            root.setId(1L); root.setItemId(1L); root.setUserId(200L);
            root.setContent("根评论"); root.setCreatedAt(java.time.LocalDateTime.now());

            MarketComment reply = new MarketComment();
            reply.setId(2L); reply.setItemId(1L); reply.setUserId(100L);
            reply.setContent("回复"); reply.setParentId(1L); reply.setReplyToUserId(200L);
            reply.setCreatedAt(java.time.LocalDateTime.now());

            when(marketCommentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(root, reply));
            when(userMapper.selectBatchIds(anySet())).thenReturn(List.of(sampleUser));

            List<MarketCommentVO> tree = marketService.getComments(1L, null);

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).getReplies()).hasSize(1);
        }

        @Test
        @DisplayName("空内容评论抛异常")
        void shouldRejectEmptyContent() {
            assertThatThrownBy(() -> marketService.addComment(1L, 100L, "", null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("参数错误");
        }

        @Test
        @DisplayName("成功添加评论")
        void shouldAddCommentSuccessfully() {
            when(marketItemMapper.selectById(1L)).thenReturn(sampleItem);
            when(marketCommentMapper.insert(any(MarketComment.class))).thenReturn(1);
            when(userMapper.selectBatchIds(anySet())).thenReturn(List.of(sampleUser));

            MarketCommentVO vo = marketService.addComment(1L, 100L, "好商品", null, null);

            assertThat(vo.getContent()).isEqualTo("好商品");
            verify(marketCommentMapper).insert(any(MarketComment.class));
        }
    }

    // ==================== offers ====================

    @Nested
    @DisplayName("出价功能")
    class Offers {

        @Test
        @DisplayName("不能给自己商品出价")
        void shouldRejectSelfOffer() {
            when(marketItemMapper.selectById(1L)).thenReturn(sampleItem);

            OfferRequest req = new OfferRequest();
            req.setPrice(new BigDecimal("80.00"));

            assertThatThrownBy(() -> marketService.makeOffer(1L, 100L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能给自己的商品");
        }

        @Test
        @DisplayName("已售商品不能出价")
        void shouldRejectOfferOnSoldItem() {
            sampleItem.setStatus("SOLD");
            when(marketItemMapper.selectById(1L)).thenReturn(sampleItem);

            OfferRequest req = new OfferRequest();
            req.setPrice(new BigDecimal("80.00"));

            assertThatThrownBy(() -> marketService.makeOffer(1L, 200L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已售出");
        }

        @Test
        @DisplayName("成功出价")
        void shouldMakeOfferSuccessfully() {
            when(marketItemMapper.selectById(1L)).thenReturn(sampleItem);
            when(marketOfferMapper.insert(any(MarketOffer.class))).thenReturn(1);

            OfferRequest req = new OfferRequest();
            req.setPrice(new BigDecimal("80.00"));
            req.setMessage("便宜点");

            assertThatCode(() -> marketService.makeOffer(1L, 200L, req))
                    .doesNotThrowAnyException();
            verify(marketOfferMapper).insert(any(MarketOffer.class));
        }

        @Test
        @DisplayName("接受出价时验证所有权")
        void shouldCheckOwnershipOnAccept() {
            MarketOffer offer = new MarketOffer();
            offer.setId(1L); offer.setItemId(1L);
            offer.setBuyerId(200L); offer.setSellerId(100L);
            offer.setPrice(new BigDecimal("80.00")); offer.setStatus("PENDING");
            sampleItem.setUserId(999L); // not the owner

            when(marketOfferMapper.selectById(1L)).thenReturn(offer);
            when(marketItemMapper.selectById(1L)).thenReturn(sampleItem);

            assertThatThrownBy(() -> marketService.acceptOffer(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权");
            sampleItem.setUserId(100L); // restore
        }

        @Test
        @DisplayName("拒绝出价时商品不存在抛 ITEM_NOT_FOUND")
        void shouldThrowItemNotFoundOnReject() {
            MarketOffer offer = new MarketOffer();
            offer.setId(1L); offer.setItemId(999L);
            offer.setStatus("PENDING");

            when(marketOfferMapper.selectById(1L)).thenReturn(offer);
            when(marketItemMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> marketService.rejectOffer(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("商品不存在");
        }
    }
}
