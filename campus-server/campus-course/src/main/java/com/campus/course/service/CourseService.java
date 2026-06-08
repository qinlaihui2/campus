package com.campus.course.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.course.dto.*;
import com.campus.course.entity.Course;
import com.campus.course.entity.CourseChapter;

import java.util.List;

public interface CourseService {

    Page<CourseVO> listCourses(String category, int page, int size, Long currentUserId);

    CourseDetailVO getDetail(Long id, Long currentUserId);

    boolean toggleLike(Long courseId, Long userId);

    boolean toggleFavorite(Long courseId, Long userId);

    Page<CourseVO> listFavorites(int page, int size, Long userId);

    List<CommentVO> getComments(Long courseId, String sort, Long currentUserId);

    CommentVO addComment(Long courseId, Long userId, String content, Long parentId, Long replyToUserId);

    boolean toggleCommentLike(Long commentId, Long userId);

    void deleteComment(Long commentId, Long userId);

    // Admin
    void createCourse(Course course);

    void updateCourse(Long id, Course course);

    void deleteCourse(Long id);

    void addChapter(Long courseId, CourseChapter chapter);

    void updateChapter(Long courseId, Long chapterId, CourseChapter chapter);

    void deleteChapter(Long courseId, Long chapterId);
}
