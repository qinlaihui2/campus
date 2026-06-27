package com.campus.course.controller;

import com.campus.common.result.R;
import com.campus.course.entity.Course;
import com.campus.course.entity.CourseChapter;
import com.campus.course.entity.CourseVideo;
import com.campus.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final CourseService courseService;

    @PostMapping
    public R<String> create(@RequestBody Course course) {
        courseService.createCourse(course);
        return R.ok("创建成功");
    }

    @PutMapping("/{id}")
    public R<String> update(@PathVariable Long id, @RequestBody Course course) {
        courseService.updateCourse(id, course);
        return R.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return R.ok("删除成功");
    }

    @PostMapping("/{id}/chapters")
    public R<String> addChapter(@PathVariable Long id, @RequestBody CourseChapter chapter) {
        courseService.addChapter(id, chapter);
        return R.ok("添加成功");
    }

    @PutMapping("/{id}/chapters/{chapterId}")
    public R<String> updateChapter(@PathVariable Long id, @PathVariable Long chapterId,
                                   @RequestBody CourseChapter chapter) {
        courseService.updateChapter(id, chapterId, chapter);
        return R.ok("更新成功");
    }

    @DeleteMapping("/{id}/chapters/{chapterId}")
    public R<String> deleteChapter(@PathVariable Long id, @PathVariable Long chapterId) {
        courseService.deleteChapter(id, chapterId);
        return R.ok("删除成功");
    }

    // ========== Video ==========

    @PostMapping("/{courseId}/chapters/{chapterId}/videos")
    public R<String> addVideo(@PathVariable Long courseId, @PathVariable Long chapterId,
                               @RequestBody CourseVideo video) {
        courseService.addVideo(courseId, chapterId, video);
        return R.ok("添加成功");
    }

    @PutMapping("/{courseId}/chapters/{chapterId}/videos/{videoId}")
    public R<String> updateVideo(@PathVariable Long courseId, @PathVariable Long chapterId,
                                  @PathVariable Long videoId, @RequestBody CourseVideo video) {
        courseService.updateVideo(courseId, chapterId, videoId, video);
        return R.ok("更新成功");
    }

    @DeleteMapping("/{courseId}/chapters/{chapterId}/videos/{videoId}")
    public R<String> deleteVideo(@PathVariable Long courseId, @PathVariable Long chapterId,
                                  @PathVariable Long videoId) {
        courseService.deleteVideo(courseId, chapterId, videoId);
        return R.ok("删除成功");
    }
}
