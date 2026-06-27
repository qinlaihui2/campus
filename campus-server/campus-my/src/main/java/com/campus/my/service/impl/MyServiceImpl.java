package com.campus.my.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.course.entity.Course;
import com.campus.course.entity.CourseComment;
import com.campus.course.entity.CourseCommentLike;
import com.campus.course.entity.CourseFavorite;
import com.campus.course.entity.CourseLike;
import com.campus.course.mapper.CourseCommentLikeMapper;
import com.campus.course.mapper.CourseCommentMapper;
import com.campus.course.mapper.CourseFavoriteMapper;
import com.campus.course.mapper.CourseLikeMapper;
import com.campus.course.mapper.CourseMapper;
import com.campus.market.entity.MarketItem;
import com.campus.market.entity.MarketLike;
import com.campus.market.mapper.MarketItemMapper;
import com.campus.market.mapper.MarketLikeMapper;
import com.campus.my.dto.MyItemVO;
import com.campus.my.service.MyService;
import com.campus.square.entity.SquareFavorite;
import com.campus.square.entity.SquareLike;
import com.campus.square.entity.SquarePost;
import com.campus.square.mapper.SquareFavoriteMapper;
import com.campus.square.mapper.SquareLikeMapper;
import com.campus.square.mapper.SquarePostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyServiceImpl implements MyService {

    private final CourseLikeMapper courseLikeMapper;
    private final SquareLikeMapper squareLikeMapper;
    private final CourseCommentLikeMapper courseCommentLikeMapper;
    private final CourseFavoriteMapper courseFavoriteMapper;
    private final CourseMapper courseMapper;
    private final SquarePostMapper squarePostMapper;
    private final SquareFavoriteMapper squareFavoriteMapper;
    private final CourseCommentMapper courseCommentMapper;
    private final MarketLikeMapper marketLikeMapper;
    private final MarketItemMapper marketItemMapper;

    @Override
    public List<MyItemVO> listLikes(Long userId, int page, int size) {
        List<MyItemVO> allItems = new ArrayList<>();

        // 1. 课程点赞
        List<CourseLike> courseLikes = courseLikeMapper.selectList(
                new LambdaQueryWrapper<CourseLike>()
                        .eq(CourseLike::getUserId, userId)
                        .orderByDesc(CourseLike::getCreatedAt));
        if (!courseLikes.isEmpty()) {
            List<Long> courseIds = courseLikes.stream().map(CourseLike::getCourseId).toList();
            Map<Long, Course> courseMap = courseMapper.selectBatchIds(courseIds).stream()
                    .collect(Collectors.toMap(Course::getId, c -> c));
            for (CourseLike cl : courseLikes) {
                Course c = courseMap.get(cl.getCourseId());
                if (c == null || c.getStatus() != 1) continue;
                MyItemVO vo = new MyItemVO();
                vo.setId(cl.getId());
                vo.setType("course");
                vo.setTargetId(c.getId());
                vo.setTitle(c.getTitle());
                vo.setDescription(c.getInstructor() + " · " + c.getCategory());
                vo.setCoverImage(c.getCoverImage());
                vo.setLikeCount(c.getLikeCount());
                vo.setCreatedAt(cl.getCreatedAt());
                allItems.add(vo);
            }
        }

        // 2. 广场帖子点赞
        List<SquareLike> squareLikes = squareLikeMapper.selectList(
                new LambdaQueryWrapper<SquareLike>()
                        .eq(SquareLike::getUserId, userId)
                        .orderByDesc(SquareLike::getCreatedAt));
        if (!squareLikes.isEmpty()) {
            List<Long> postIds = squareLikes.stream().map(SquareLike::getPostId).toList();
            Map<Long, SquarePost> postMap = squarePostMapper.selectBatchIds(postIds).stream()
                    .collect(Collectors.toMap(SquarePost::getId, p -> p));
            for (SquareLike sl : squareLikes) {
                SquarePost p = postMap.get(sl.getPostId());
                if (p == null || p.getStatus() != 1) continue;
                MyItemVO vo = new MyItemVO();
                vo.setId(sl.getId());
                vo.setType("square_post");
                vo.setTargetId(p.getId());
                vo.setTitle(p.getTitle());
                vo.setDescription(truncate(p.getQuestion(), 60));
                vo.setCoverImage(null);
                vo.setLikeCount(p.getLikeCount());
                vo.setCreatedAt(sl.getCreatedAt());
                allItems.add(vo);
            }
        }

        // 3. 课程评论点赞（关联到对应课程）
        List<CourseCommentLike> commentLikes = courseCommentLikeMapper.selectList(
                new LambdaQueryWrapper<CourseCommentLike>()
                        .eq(CourseCommentLike::getUserId, userId)
                        .orderByDesc(CourseCommentLike::getCreatedAt));
        if (!commentLikes.isEmpty()) {
            List<Long> commentIds = commentLikes.stream().map(CourseCommentLike::getCommentId).toList();
            Map<Long, CourseComment> commentMap = courseCommentMapper.selectBatchIds(commentIds).stream()
                    .collect(Collectors.toMap(CourseComment::getId, c -> c));
            List<Long> courseIds = commentMap.values().stream()
                    .map(CourseComment::getCourseId).distinct().toList();
            Map<Long, Course> courseMap = courseMapper.selectBatchIds(courseIds).stream()
                    .collect(Collectors.toMap(Course::getId, c -> c));
            for (CourseCommentLike ccl : commentLikes) {
                CourseComment comment = commentMap.get(ccl.getCommentId());
                if (comment == null) continue;
                Course c = courseMap.get(comment.getCourseId());
                if (c == null || c.getStatus() != 1) continue;
                MyItemVO vo = new MyItemVO();
                vo.setId(ccl.getId());
                vo.setType("course");
                vo.setTargetId(c.getId());
                vo.setTitle("评论了：" + truncate(c.getTitle(), 30));
                vo.setDescription(truncate(comment.getContent(), 60));
                vo.setCoverImage(c.getCoverImage());
                vo.setLikeCount(comment.getLikeCount());
                vo.setCreatedAt(ccl.getCreatedAt());
                allItems.add(vo);
            }
        }

        // 4. 市场商品点赞（收藏）
        List<MarketLike> marketLikes = marketLikeMapper.selectList(
                new LambdaQueryWrapper<MarketLike>()
                        .eq(MarketLike::getUserId, userId)
                        .orderByDesc(MarketLike::getCreatedAt));
        if (!marketLikes.isEmpty()) {
            List<Long> itemIds = marketLikes.stream().map(MarketLike::getItemId).toList();
            Map<Long, MarketItem> itemMap = marketItemMapper.selectBatchIds(itemIds).stream()
                    .collect(Collectors.toMap(MarketItem::getId, m -> m));
            for (MarketLike ml : marketLikes) {
                MarketItem m = itemMap.get(ml.getItemId());
                if (m == null || "REMOVED".equals(m.getStatus())) continue;
                MyItemVO vo = new MyItemVO();
                vo.setId(ml.getId());
                vo.setType("market");
                vo.setTargetId(m.getId());
                vo.setTitle(m.getTitle());
                vo.setDescription("¥" + m.getPrice() + " · " + (m.getCategory() != null ? m.getCategory() : ""));
                vo.setCoverImage(m.getImages());
                vo.setLikeCount(m.getLikeCount());
                vo.setCreatedAt(ml.getCreatedAt());
                allItems.add(vo);
            }
        }

        // 按时间倒序 + 内存分页
        allItems.sort(Comparator.comparing(MyItemVO::getCreatedAt).reversed());
        int start = (page - 1) * size;
        if (start >= allItems.size()) return List.of();
        int end = Math.min(start + size, allItems.size());
        return allItems.subList(start, end);
    }

    @Override
    public List<MyItemVO> listFavorites(Long userId, int page, int size) {
        List<MyItemVO> allItems = new ArrayList<>();

        // 1. 课程收藏
        List<CourseFavorite> courseFavs = courseFavoriteMapper.selectList(
                new LambdaQueryWrapper<CourseFavorite>()
                        .eq(CourseFavorite::getUserId, userId)
                        .orderByDesc(CourseFavorite::getCreatedAt));
        if (!courseFavs.isEmpty()) {
            List<Long> courseIds = courseFavs.stream().map(CourseFavorite::getCourseId).toList();
            Map<Long, Course> courseMap = courseMapper.selectBatchIds(courseIds).stream()
                    .collect(Collectors.toMap(Course::getId, c -> c));
            for (CourseFavorite fav : courseFavs) {
                Course c = courseMap.get(fav.getCourseId());
                if (c == null || c.getStatus() != 1) continue;
                MyItemVO vo = new MyItemVO();
                vo.setId(fav.getId());
                vo.setType("course");
                vo.setTargetId(c.getId());
                vo.setTitle(c.getTitle());
                vo.setDescription(c.getInstructor() + " · " + c.getCategory());
                vo.setCoverImage(c.getCoverImage());
                vo.setLikeCount(c.getFavoriteCount());
                vo.setCreatedAt(fav.getCreatedAt());
                allItems.add(vo);
            }
        }

        // 2. 广场帖子收藏
        List<SquareFavorite> squareFavs = squareFavoriteMapper.selectList(
                new LambdaQueryWrapper<SquareFavorite>()
                        .eq(SquareFavorite::getUserId, userId)
                        .orderByDesc(SquareFavorite::getCreatedAt));
        if (!squareFavs.isEmpty()) {
            List<Long> postIds = squareFavs.stream().map(SquareFavorite::getPostId).toList();
            Map<Long, SquarePost> postMap = squarePostMapper.selectBatchIds(postIds).stream()
                    .collect(Collectors.toMap(SquarePost::getId, p -> p));
            for (SquareFavorite sf : squareFavs) {
                SquarePost p = postMap.get(sf.getPostId());
                if (p == null || p.getStatus() != 1) continue;
                MyItemVO vo = new MyItemVO();
                vo.setId(sf.getId());
                vo.setType("square_post");
                vo.setTargetId(p.getId());
                vo.setTitle(p.getTitle());
                vo.setDescription(truncate(p.getQuestion(), 60));
                vo.setCoverImage(null);
                vo.setLikeCount(p.getLikeCount());
                vo.setCreatedAt(sf.getCreatedAt());
                allItems.add(vo);
            }
        }

        // 按时间倒序 + 内存分页
        allItems.sort(Comparator.comparing(MyItemVO::getCreatedAt).reversed());
        int start = (page - 1) * size;
        if (start >= allItems.size()) return List.of();
        int end = Math.min(start + size, allItems.size());
        return allItems.subList(start, end);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
