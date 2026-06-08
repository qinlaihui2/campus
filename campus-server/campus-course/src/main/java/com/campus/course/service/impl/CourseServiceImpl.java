package com.campus.course.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.entity.User;
import com.campus.common.exception.BusinessException;
import com.campus.common.mapper.UserMapper;
import com.campus.common.result.ResultCode;
import com.campus.course.dto.*;
import com.campus.course.entity.*;
import com.campus.course.mapper.*;
import com.campus.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseChapterMapper chapterMapper;
    private final CourseCommentMapper commentMapper;
    private final CourseCommentLikeMapper commentLikeMapper;
    private final CourseLikeMapper likeMapper;
    private final CourseFavoriteMapper favoriteMapper;
    private final UserMapper userMapper;

    @Override
    public Page<CourseVO> listCourses(String category, int page, int size, Long currentUserId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, 1)
                .orderByDesc(Course::getCreatedAt);
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(Course::getCategory, category);
        }
        Page<Course> pageParam = new Page<>(page, size);
        Page<Course> result = this.page(pageParam, wrapper);
        return convertPage(result, currentUserId);
    }

    @Override
    public CourseDetailVO getDetail(Long id, Long currentUserId) {
        Course course = this.getById(id);
        if (course == null || course.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 原子递增，避免并发竞态
        this.update(new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, id)
                .setSql("view_count = view_count + 1"));
        course.setViewCount(course.getViewCount() + 1); // 同步当前对象

        CourseDetailVO vo = new CourseDetailVO();
        fillCourseVO(vo, course, currentUserId);

        List<CourseChapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourseId, id)
                        .eq(CourseChapter::getDeleted, 0)
                        .orderByAsc(CourseChapter::getSortOrder));
        vo.setChapters(chapters.stream().map(ch -> {
            ChapterVO cv = new ChapterVO();
            cv.setId(ch.getId());
            cv.setTitle(ch.getTitle());
            cv.setDescription(ch.getDescription());
            cv.setVideoUrl(ch.getVideoUrl());
            cv.setDuration(ch.getDuration());
            cv.setSortOrder(ch.getSortOrder());
            return cv;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Override
    @Transactional
    public boolean toggleLike(Long courseId, Long userId) {
        Course course = this.getById(courseId);
        if (course == null) throw new BusinessException(ResultCode.NOT_FOUND);

        LambdaQueryWrapper<CourseLike> wrapper = new LambdaQueryWrapper<CourseLike>()
                .eq(CourseLike::getCourseId, courseId)
                .eq(CourseLike::getUserId, userId);
        CourseLike existing = likeMapper.selectOne(wrapper);

        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            course.setLikeCount(Math.max(0, course.getLikeCount() - 1));
            this.updateById(course);
            return false;
        } else {
            CourseLike like = new CourseLike();
            like.setCourseId(courseId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            likeMapper.insert(like);
            course.setLikeCount(course.getLikeCount() + 1);
            this.updateById(course);
            return true;
        }
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long courseId, Long userId) {
        Course course = this.getById(courseId);
        if (course == null) throw new BusinessException(ResultCode.NOT_FOUND);

        LambdaQueryWrapper<CourseFavorite> wrapper = new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getCourseId, courseId)
                .eq(CourseFavorite::getUserId, userId);
        CourseFavorite existing = favoriteMapper.selectOne(wrapper);

        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            course.setFavoriteCount(Math.max(0, course.getFavoriteCount() - 1));
            this.updateById(course);
            return false;
        } else {
            CourseFavorite fav = new CourseFavorite();
            fav.setCourseId(courseId);
            fav.setUserId(userId);
            fav.setCreatedAt(LocalDateTime.now());
            favoriteMapper.insert(fav);
            course.setFavoriteCount(course.getFavoriteCount() + 1);
            this.updateById(course);
            return true;
        }
    }

    @Override
    public Page<CourseVO> listFavorites(int page, int size, Long userId) {
        LambdaQueryWrapper<CourseFavorite> favWrapper = new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getUserId, userId)
                .orderByDesc(CourseFavorite::getCreatedAt);
        Page<CourseFavorite> favPage = new Page<>(page, size);
        Page<CourseFavorite> favResult = favoriteMapper.selectPage(favPage, favWrapper);

        List<Long> courseIds = favResult.getRecords().stream()
                .map(CourseFavorite::getCourseId).collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return new Page<>(page, size, 0);
        }

        List<Course> courses = this.listByIds(courseIds);
        Page<CourseVO> voPage = new Page<>(page, size, favResult.getTotal());
        voPage.setRecords(courses.stream()
                .map(c -> toVO(c, userId))
                .collect(Collectors.toList()));
        return voPage;
    }

    // ========== 评论（复用 square 评论的模式） ==========

    @Override
    public List<CommentVO> getComments(Long courseId, String sort, Long currentUserId) {
        LambdaQueryWrapper<CourseComment> wrapper = new LambdaQueryWrapper<CourseComment>()
                .eq(CourseComment::getCourseId, courseId)
                .eq(CourseComment::getDeleted, 0);

        if ("newest".equals(sort)) {
            wrapper.orderByDesc(CourseComment::getCreatedAt);
        } else if ("oldest".equals(sort)) {
            wrapper.orderByAsc(CourseComment::getCreatedAt);
        } else {
            wrapper.orderByDesc(CourseComment::getLikeCount)
                   .orderByDesc(CourseComment::getCreatedAt);
        }

        List<CourseComment> all = commentMapper.selectList(wrapper);
        return buildCommentTree(all, currentUserId);
    }

    private List<CommentVO> buildCommentTree(List<CourseComment> all, Long currentUserId) {
        return all.stream()
                .filter(c -> c.getParentId() == null)
                .map(root -> buildNode(root, all, currentUserId))
                .collect(Collectors.toList());
    }

    private CommentVO buildNode(CourseComment comment, List<CourseComment> all, Long currentUserId) {
        CommentVO vo = toCommentVO(comment, currentUserId);
        List<CommentVO> children = all.stream()
                .filter(c -> comment.getId().equals(c.getParentId()))
                .map(c -> buildNode(c, all, currentUserId))
                .collect(Collectors.toList());
        vo.setReplies(children.isEmpty() ? null : children);
        return vo;
    }

    @Override
    @Transactional
    public CommentVO addComment(Long courseId, Long userId, String content, Long parentId, Long replyToUserId) {
        CourseComment comment = new CourseComment();
        comment.setCourseId(courseId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setLikeCount(0);
        comment.setDeleted(0);
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        return toCommentVO(comment, userId);
    }

    @Override
    @Transactional
    public boolean toggleCommentLike(Long commentId, Long userId) {
        CourseComment comment = commentMapper.selectById(commentId);
        if (comment == null) throw new BusinessException(ResultCode.NOT_FOUND);

        LambdaQueryWrapper<CourseCommentLike> wrapper = new LambdaQueryWrapper<CourseCommentLike>()
                .eq(CourseCommentLike::getCommentId, commentId)
                .eq(CourseCommentLike::getUserId, userId);
        CourseCommentLike existing = commentLikeMapper.selectOne(wrapper);

        if (existing != null) {
            commentLikeMapper.deleteById(existing.getId());
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentMapper.updateById(comment);
            return false;
        } else {
            CourseCommentLike like = new CourseCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            commentLikeMapper.insert(like);
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentMapper.updateById(comment);
            return true;
        }
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        CourseComment comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        comment.setDeleted(1);
        commentMapper.updateById(comment);
    }

    // ========== Admin ==========

    @Override
    public void createCourse(Course course) {
        course.setViewCount(0);
        course.setLikeCount(0);
        course.setFavoriteCount(0);
        course.setStatus(1);
        this.save(course);
    }

    @Override
    public void updateCourse(Long id, Course course) {
        course.setId(id);
        this.updateById(course);
    }

    @Override
    public void deleteCourse(Long id) {
        Course course = this.getById(id);
        if (course != null) {
            course.setStatus(0);
            this.updateById(course);
        }
    }

    @Override
    public void addChapter(Long courseId, CourseChapter chapter) {
        chapter.setCourseId(courseId);
        chapter.setCreatedAt(LocalDateTime.now());
        chapterMapper.insert(chapter);
    }

    @Override
    public void updateChapter(Long courseId, Long chapterId, CourseChapter chapter) {
        chapter.setId(chapterId);
        chapter.setCourseId(courseId);
        chapterMapper.updateById(chapter);
    }

    @Override
    public void deleteChapter(Long courseId, Long chapterId) {
        CourseChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter != null) {
            chapter.setDeleted(1);
            chapterMapper.updateById(chapter);
        }
    }

    // ========== Helpers ==========

    private Page<CourseVO> convertPage(Page<Course> page, Long currentUserId) {
        Page<CourseVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(c -> toVO(c, currentUserId))
                .collect(Collectors.toList()));
        return voPage;
    }

    private CourseVO toVO(Course course, Long currentUserId) {
        CourseVO vo = new CourseVO();
        fillCourseVO(vo, course, currentUserId);
        return vo;
    }

    private void fillCourseVO(CourseVO vo, Course course, Long currentUserId) {
        vo.setId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setDescription(course.getDescription());
        vo.setCoverImage(course.getCoverImage());
        vo.setInstructor(course.getInstructor());
        vo.setCategory(course.getCategory());
        vo.setViewCount(course.getViewCount());
        vo.setLikeCount(course.getLikeCount());
        vo.setFavoriteCount(course.getFavoriteCount());
        vo.setCreatedAt(course.getCreatedAt());

        Long count = chapterMapper.selectCount(
                new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourseId, course.getId()));
        vo.setChapterCount(count.intValue());

        if (currentUserId != null) {
            vo.setLiked(likeMapper.selectCount(
                    new LambdaQueryWrapper<CourseLike>()
                            .eq(CourseLike::getCourseId, course.getId())
                            .eq(CourseLike::getUserId, currentUserId)) > 0);
            vo.setFavorited(favoriteMapper.selectCount(
                    new LambdaQueryWrapper<CourseFavorite>()
                            .eq(CourseFavorite::getCourseId, course.getId())
                            .eq(CourseFavorite::getUserId, currentUserId)) > 0);
        }
    }

    private CommentVO toCommentVO(CourseComment comment, Long currentUserId) {
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
            vo.setLiked(commentLikeMapper.selectCount(
                    new LambdaQueryWrapper<CourseCommentLike>()
                            .eq(CourseCommentLike::getCommentId, comment.getId())
                            .eq(CourseCommentLike::getUserId, currentUserId)) > 0);
        }
        return vo;
    }
}
