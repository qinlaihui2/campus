package com.campus.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.entity.User;
import com.campus.common.exception.BusinessException;
import com.campus.common.mapper.UserMapper;
import com.campus.course.dto.CourseDetailVO;
import com.campus.course.dto.CourseVO;
import com.campus.course.dto.CommentVO;
import com.campus.course.entity.*;
import com.campus.course.mapper.*;
import com.campus.course.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("课程服务单元测试")
class CourseServiceImplTest {

    @Mock private CourseMapper courseMapper;
    @Mock private CourseChapterMapper chapterMapper;
    @Mock private CourseCommentMapper commentMapper;
    @Mock private CourseCommentLikeMapper commentLikeMapper;
    @Mock private CourseLikeMapper likeMapper;
    @Mock private CourseFavoriteMapper favoriteMapper;
    @Mock private CourseVideoMapper videoMapper;
    @Mock private UserMapper userMapper;
    @InjectMocks private CourseServiceImpl courseService;

    private Course course;

    @BeforeEach
    void setUp() {
        // Inject baseMapper into the parent ServiceImpl
        ReflectionTestUtils.setField(courseService, "baseMapper", courseMapper);
        course = new Course();
        course.setId(1L);
        course.setTitle("Java 从入门到精通");
        course.setDescription("全面掌握 Java");
        course.setCoverImage("cover-office.png");
        course.setInstructor("张教授");
        course.setCategory("编程开发");
        course.setViewCount(100);
        course.setLikeCount(20);
        course.setFavoriteCount(10);
        course.setStatus(1);
        course.setCreatedAt(LocalDateTime.now());
    }

    @Nested @DisplayName("课程列表查询")
    class ListCourses {
        @Test @DisplayName("查询全部课程")
        void shouldListAllCourses() {
            Page<Course> page = new Page<>(1, 12, 1);
            page.setRecords(List.of(course));
            when(courseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);
            when(chapterMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
            when(likeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(favoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            Page<CourseVO> result = courseService.listCourses(null, 1, 12, null);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getTitle()).isEqualTo("Java 从入门到精通");
        }
    }

    @Nested @DisplayName("课程详情")
    class GetDetail {
        @Test @DisplayName("查询存在的课程返回详情含章节")
        void shouldReturnDetailWithChapters() {
            when(courseMapper.selectById(1L)).thenReturn(course);
            when(courseMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            CourseChapter ch = new CourseChapter();
            ch.setId(1L); ch.setCourseId(1L); ch.setTitle("第1章"); ch.setSortOrder(1);
            when(chapterMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ch));
            when(chapterMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(videoMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(likeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(favoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            CourseDetailVO vo = courseService.getDetail(1L, null);
            assertThat(vo.getTitle()).isEqualTo("Java 从入门到精通");
            assertThat(vo.getChapters()).hasSize(1);
            assertThat(vo.getViewCount()).isEqualTo(101);
        }

        @Test @DisplayName("查询不存在的课程抛出异常")
        void shouldThrowWhenNotFound() {
            when(courseMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> courseService.getDetail(999L, null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested @DisplayName("课程点赞 toggle")
    class ToggleLike {
        @Test @DisplayName("首次点赞返回 true")
        void shouldLike() {
            when(courseMapper.selectById(1L)).thenReturn(course);
            when(courseMapper.updateById(any(Course.class))).thenReturn(1);
            when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(likeMapper.insert(any(CourseLike.class))).thenReturn(1);

            boolean result = courseService.toggleLike(1L, 1L);
            assertThat(result).isTrue();
        }

        @Test @DisplayName("重复点赞取消返回 false")
        void shouldUnlike() {
            when(courseMapper.selectById(1L)).thenReturn(course);
            when(courseMapper.updateById(any(Course.class))).thenReturn(1);
            CourseLike existing = new CourseLike();
            existing.setId(1L); existing.setCourseId(1L); existing.setUserId(1L);
            when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
            when(likeMapper.deleteById(1L)).thenReturn(1);

            boolean result = courseService.toggleLike(1L, 1L);
            assertThat(result).isFalse();
        }
    }

    @Nested @DisplayName("课程收藏 toggle")
    class ToggleFavorite {
        @Test @DisplayName("首次收藏返回 true")
        void shouldFavorite() {
            when(courseMapper.selectById(1L)).thenReturn(course);
            when(courseMapper.updateById(any(Course.class))).thenReturn(1);
            when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(favoriteMapper.insert(any(CourseFavorite.class))).thenReturn(1);

            boolean result = courseService.toggleFavorite(1L, 1L);
            assertThat(result).isTrue();
        }

        @Test @DisplayName("重复收藏取消返回 false")
        void shouldUnfavorite() {
            when(courseMapper.selectById(1L)).thenReturn(course);
            when(courseMapper.updateById(any(Course.class))).thenReturn(1);
            CourseFavorite existing = new CourseFavorite();
            existing.setId(1L); existing.setCourseId(1L); existing.setUserId(1L);
            when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
            when(favoriteMapper.deleteById(1L)).thenReturn(1);

            boolean result = courseService.toggleFavorite(1L, 1L);
            assertThat(result).isFalse();
        }
    }

    @Nested @DisplayName("评论点赞 toggle")
    class ToggleCommentLike {
        @Test @DisplayName("首次点赞返回 true")
        void shouldLikeComment() {
            CourseComment comment = new CourseComment();
            comment.setId(1L); comment.setLikeCount(3);
            when(commentMapper.selectById(1L)).thenReturn(comment);
            when(commentLikeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(commentLikeMapper.insert(any(CourseCommentLike.class))).thenReturn(1);
            when(commentMapper.updateById(any(CourseComment.class))).thenReturn(1);

            boolean result = courseService.toggleCommentLike(1L, 1L);
            assertThat(result).isTrue();
        }

        @Test @DisplayName("重复点赞取消返回 false")
        void shouldUnlikeComment() {
            CourseComment comment = new CourseComment();
            comment.setId(1L); comment.setLikeCount(3);
            when(commentMapper.selectById(1L)).thenReturn(comment);
            CourseCommentLike existing = new CourseCommentLike();
            existing.setId(1L); existing.setCommentId(1L); existing.setUserId(1L);
            when(commentLikeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
            when(commentLikeMapper.deleteById(1L)).thenReturn(1);
            when(commentMapper.updateById(any(CourseComment.class))).thenReturn(1);

            boolean result = courseService.toggleCommentLike(1L, 1L);
            assertThat(result).isFalse();
        }
    }

    @Nested @DisplayName("评论 CRUD")
    class Comments {
        @Test @DisplayName("添加评论返回 VO 含用户信息")
        void shouldAddComment() {
            when(commentMapper.insert(any(CourseComment.class))).thenReturn(1);
            User user = new User();
            user.setId(1L); user.setNickname("小明");
            when(userMapper.selectById(1L)).thenReturn(user);

            CommentVO vo = courseService.addComment(1L, 1L, "好课！", null, null);
            assertThat(vo.getContent()).isEqualTo("好课！");
            assertThat(vo.getUserNickname()).isEqualTo("小明");
        }

        @Test @DisplayName("非本人删除评论抛出异常")
        void shouldRejectDeleteByOthers() {
            CourseComment comment = new CourseComment();
            comment.setId(1L); comment.setUserId(100L);
            when(commentMapper.selectById(1L)).thenReturn(comment);

            assertThatThrownBy(() -> courseService.deleteComment(1L, 999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested @DisplayName("Admin 软删除")
    class AdminDelete {
        @Test @DisplayName("删除课程设置 status=0")
        void shouldSoftDelete() {
            when(courseMapper.selectById(1L)).thenReturn(course);
            when(courseMapper.updateById(any(Course.class))).thenReturn(1);

            courseService.deleteCourse(1L);
            assertThat(course.getStatus()).isEqualTo(0);
        }
    }
}
