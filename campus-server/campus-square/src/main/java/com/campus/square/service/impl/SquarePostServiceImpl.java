package com.campus.square.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.entity.User;
import com.campus.common.exception.BusinessException;
import com.campus.common.mapper.UserMapper;
import com.campus.common.notification.NotificationEvent;
import com.campus.common.notification.NotificationHelper;
import com.campus.common.result.ResultCode;
import com.campus.square.dto.CommentVO;
import com.campus.square.entity.SquareComment;
import com.campus.square.entity.SquareCommentLike;
import com.campus.square.mapper.SquareCommentMapper;
import com.campus.square.mapper.SquareCommentLikeMapper;
import com.campus.square.dto.PublishRequest;
import com.campus.square.dto.SquarePostVO;
import com.campus.square.entity.SquareFavorite;
import com.campus.square.entity.SquareLike;
import com.campus.square.entity.SquarePost;
import com.campus.square.mapper.SquareFavoriteMapper;
import com.campus.square.mapper.SquareLikeMapper;
import com.campus.square.mapper.SquarePostMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SquarePostServiceImpl extends ServiceImpl<SquarePostMapper, SquarePost> implements com.campus.square.service.SquarePostService {

    private final SquareLikeMapper squareLikeMapper;
    private final SquareFavoriteMapper squareFavoriteMapper;
    private final SquareCommentMapper squareCommentMapper;
    private final SquareCommentLikeMapper squareCommentLikeMapper;
    private final UserMapper userMapper;
    private final RedissonClient redissonClient;
    private final NotificationHelper notificationHelper;

    @Override
    public Page<SquarePostVO> listPosts(String category, String keyword, int page, int size, Long currentUserId) {
        LambdaQueryWrapper<SquarePost> wrapper = new LambdaQueryWrapper<SquarePost>()
                .eq(SquarePost::getStatus, 1)
                .orderByDesc(SquarePost::getCreatedAt);

        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(SquarePost::getCategory, category);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SquarePost::getTitle, keyword).or().like(SquarePost::getQuestion, keyword));
        }

        Page<SquarePost> pageParam = new Page<>(page, size);
        Page<SquarePost> result = this.page(pageParam, wrapper);
        return convertToVO(result, currentUserId);
    }

    @Override
    public Page<SquarePostVO> listHot(int page, int size, Long currentUserId) {
        String cacheKey = "square:hot";
        RSet<Long> hotSet = redissonClient.getSet(cacheKey);

        if (!hotSet.isExists()) {
            refreshHotCache();
        }

        Set<Long> hotIds = hotSet.readAll();
        if (hotIds.isEmpty()) {
            return listPosts(null, null, page, size, currentUserId);
        }

        List<SquarePost> posts = hotIds.stream()
                .map(this::getById)
                .filter(p -> p != null && p.getStatus() == 1)
                .collect(Collectors.toList());

        int total = posts.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        if (fromIndex >= total) {
            return new Page<>(page, size, total);
        }

        List<SquarePost> pagePosts = posts.subList(fromIndex, toIndex);
        Page<SquarePost> pageResult = new Page<>(page, size, total);
        pageResult.setRecords(pagePosts);
        return convertToVO(pageResult, currentUserId);
    }

    @Override
    public SquarePostVO getDetail(Long id, Long currentUserId) {
        SquarePost post = this.getById(id);
        if (post == null || post.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        post.setViewCount(post.getViewCount() + 1);
        this.updateById(post);

        return toVO(post, currentUserId);
    }

    @Override
    @Transactional
    public void publish(PublishRequest request, Long userId) {
        SquarePost post = new SquarePost();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setQuestion(request.getQuestion());
        post.setAnswer(request.getAnswer());
        post.setReferencesJson(request.getReferencesJson());
        post.setCategory(request.getCategory());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setStatus(1);
        this.save(post);
    }

    @Override
    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        SquarePost post = this.getById(postId);
        if (post == null || post.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        LambdaQueryWrapper<SquareLike> wrapper = new LambdaQueryWrapper<SquareLike>()
                .eq(SquareLike::getPostId, postId)
                .eq(SquareLike::getUserId, userId);
        SquareLike existing = squareLikeMapper.selectOne(wrapper);

        if (existing != null) {
            squareLikeMapper.deleteById(existing.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            this.updateById(post);
            return false;
        } else {
            SquareLike like = new SquareLike();
            like.setPostId(postId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            squareLikeMapper.insert(like);
            post.setLikeCount(post.getLikeCount() + 1);
            this.updateById(post);

            // 通知帖子主人
            if (!post.getUserId().equals(userId)) {
                notificationHelper.send(NotificationEvent.builder()
                        .type("LIKE")
                        .userId(post.getUserId())
                        .title("有人赞了你的帖子")
                        .content("你的帖子《" + post.getTitle() + "》收到一个新赞")
                        .targetType("POST")
                        .targetId(postId)
                        .build());
            }

            return true;
        }
    }

    @Override
    public Page<SquarePostVO> listMyPosts(int page, int size, Long userId) {
        Page<SquarePost> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SquarePost> wrapper = new LambdaQueryWrapper<SquarePost>()
                .eq(SquarePost::getUserId, userId)
                .orderByDesc(SquarePost::getCreatedAt);
        Page<SquarePost> result = this.page(pageParam, wrapper);
        return convertToVO(result, userId);
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long postId, Long userId) {
        SquarePost post = this.getById(postId);
        if (post == null || post.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        LambdaQueryWrapper<SquareFavorite> wrapper = new LambdaQueryWrapper<SquareFavorite>()
                .eq(SquareFavorite::getPostId, postId)
                .eq(SquareFavorite::getUserId, userId);
        SquareFavorite existing = squareFavoriteMapper.selectOne(wrapper);

        if (existing != null) {
            squareFavoriteMapper.deleteById(existing.getId());
            return false;
        } else {
            SquareFavorite fav = new SquareFavorite();
            fav.setPostId(postId);
            fav.setUserId(userId);
            fav.setCreatedAt(LocalDateTime.now());
            squareFavoriteMapper.insert(fav);
            return true;
        }
    }

    @Override
    public void deletePost(Long id) {
        SquarePost post = this.getById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        this.removeById(id);
    }

    private void refreshHotCache() {
        LambdaQueryWrapper<SquarePost> wrapper = new LambdaQueryWrapper<SquarePost>()
                .eq(SquarePost::getStatus, 1)
                .orderByDesc(SquarePost::getLikeCount)
                .last("LIMIT 100");
        List<SquarePost> hotPosts = this.list(wrapper);

        RSet<Long> hotSet = redissonClient.getSet("square:hot");
        hotSet.delete();
        if (!hotPosts.isEmpty()) {
            hotSet.addAll(hotPosts.stream().map(SquarePost::getId).collect(Collectors.toList()));
            hotSet.expire(Duration.ofMinutes(10));
        }
    }

    private Page<SquarePostVO> convertToVO(Page<SquarePost> page, Long currentUserId) {
        Page<SquarePostVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(p -> toVO(p, currentUserId))
                .collect(Collectors.toList()));
        return voPage;
    }

    private SquarePostVO toVO(SquarePost post, Long currentUserId) {
        SquarePostVO vo = new SquarePostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setTitle(post.getTitle());
        vo.setQuestion(post.getQuestion());
        vo.setAnswer(post.getAnswer());
        vo.setReferencesJson(post.getReferencesJson());
        vo.setCategory(post.getCategory());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCreatedAt(post.getCreatedAt());

        User user = userMapper.selectById(post.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname() != null ? user.getNickname() : "同学");
            vo.setUserAvatar(user.getAvatar());
        }

        if (currentUserId != null) {
            LambdaQueryWrapper<SquareLike> wrapper = new LambdaQueryWrapper<SquareLike>()
                    .eq(SquareLike::getPostId, post.getId())
                    .eq(SquareLike::getUserId, currentUserId);
            vo.setLiked(squareLikeMapper.selectCount(wrapper) > 0);

            try {
                LambdaQueryWrapper<SquareFavorite> favWrapper = new LambdaQueryWrapper<SquareFavorite>()
                        .eq(SquareFavorite::getPostId, post.getId())
                        .eq(SquareFavorite::getUserId, currentUserId);
                vo.setFavorited(squareFavoriteMapper.selectCount(favWrapper) > 0);
            } catch (Exception e) {
                vo.setFavorited(false);
            }
        }

        return vo;
    }

    // ========== 评论 ==========

    @Override
    public List<CommentVO> getComments(Long postId, String sort, Long currentUserId) {
        LambdaQueryWrapper<SquareComment> wrapper = new LambdaQueryWrapper<SquareComment>()
                .eq(SquareComment::getPostId, postId)
                .eq(SquareComment::getDeleted, 0);

        if ("newest".equals(sort)) {
            wrapper.orderByDesc(SquareComment::getCreatedAt);
        } else if ("oldest".equals(sort)) {
            wrapper.orderByAsc(SquareComment::getCreatedAt);
        } else {
            // "hot" or default: sort by like_count desc, then created_at desc
            wrapper.orderByDesc(SquareComment::getLikeCount)
                   .orderByDesc(SquareComment::getCreatedAt);
        }

        List<SquareComment> all = squareCommentMapper.selectList(wrapper);
        return buildCommentTree(all, currentUserId);
    }

    private List<CommentVO> buildCommentTree(List<SquareComment> all, Long currentUserId) {
        return all.stream()
                .filter(c -> c.getParentId() == null)
                .map(root -> buildCommentNode(root, all, currentUserId))
                .collect(Collectors.toList());
    }

    private CommentVO buildCommentNode(SquareComment comment, List<SquareComment> all, Long currentUserId) {
        CommentVO vo = toCommentVO(comment, currentUserId);
        List<CommentVO> children = all.stream()
                .filter(c -> comment.getId().equals(c.getParentId()))
                .map(child -> buildCommentNode(child, all, currentUserId))
                .collect(Collectors.toList());
        vo.setReplies(children.isEmpty() ? null : children);
        return vo;
    }

    @Override
    @Transactional
    public CommentVO addComment(Long postId, Long userId, String content, Long parentId, Long replyToUserId) {
        SquarePost post = this.getById(postId);
        if (post == null || post.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        SquareComment comment = new SquareComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setLikeCount(0);
        comment.setDeleted(0);
        comment.setCreatedAt(java.time.LocalDateTime.now());
        squareCommentMapper.insert(comment);

        // 通知帖子主人
        if (!post.getUserId().equals(userId)) {
            String shortContent = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            notificationHelper.send(NotificationEvent.builder()
                    .type(parentId != null ? "REPLY" : "COMMENT")
                    .userId(post.getUserId())
                    .title(parentId != null ? "有人回复了你的评论" : "有人评论了你的帖子")
                    .content(shortContent)
                    .targetType("POST")
                    .targetId(postId)
                    .build());
        }

        // 如果回复别人的评论，通知被回复的人
        if (parentId != null && replyToUserId != null && !replyToUserId.equals(userId) && !replyToUserId.equals(post.getUserId())) {
            String shortContent = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            notificationHelper.send(NotificationEvent.builder()
                    .type("REPLY")
                    .userId(replyToUserId)
                    .title("有人回复了你的评论")
                    .content(shortContent)
                    .targetType("POST")
                    .targetId(postId)
                    .build());
        }

        return toCommentVO(comment, userId);
    }

    @Override
    @Transactional
    public boolean toggleCommentLike(Long commentId, Long userId) {
        LambdaQueryWrapper<SquareCommentLike> wrapper = new LambdaQueryWrapper<SquareCommentLike>()
                .eq(SquareCommentLike::getCommentId, commentId)
                .eq(SquareCommentLike::getUserId, userId);
        SquareCommentLike existing = squareCommentLikeMapper.selectOne(wrapper);

        SquareComment comment = squareCommentMapper.selectById(commentId);
        if (comment == null) throw new BusinessException(ResultCode.NOT_FOUND);

        if (existing != null) {
            squareCommentLikeMapper.deleteById(existing.getId());
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            squareCommentMapper.updateById(comment);
            return false;
        } else {
            SquareCommentLike like = new SquareCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreatedAt(java.time.LocalDateTime.now());
            squareCommentLikeMapper.insert(like);
            comment.setLikeCount(comment.getLikeCount() + 1);
            squareCommentMapper.updateById(comment);
            return true;
        }
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        SquareComment comment = squareCommentMapper.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        comment.setDeleted(1);
        squareCommentMapper.updateById(comment);
    }

    private CommentVO toCommentVO(SquareComment comment, Long currentUserId) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setReplyToUserId(comment.getReplyToUserId());

        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname() != null ? user.getNickname() : "同学");
            vo.setUserAvatar(user.getAvatar());
        }
        if (comment.getReplyToUserId() != null) {
            User replyTo = userMapper.selectById(comment.getReplyToUserId());
            if (replyTo != null) {
                vo.setReplyToUserNickname(replyTo.getNickname() != null ? replyTo.getNickname() : "同学");
            }
        }
        if (currentUserId != null) {
            LambdaQueryWrapper<SquareCommentLike> likeWrapper = new LambdaQueryWrapper<SquareCommentLike>()
                    .eq(SquareCommentLike::getCommentId, comment.getId())
                    .eq(SquareCommentLike::getUserId, currentUserId);
            vo.setLiked(squareCommentLikeMapper.selectCount(likeWrapper) > 0);
        }
        return vo;
    }
}
