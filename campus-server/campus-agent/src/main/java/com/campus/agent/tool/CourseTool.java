package com.campus.agent.tool;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.course.service.CourseService;
import com.campus.course.dto.CourseVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseTool {

    private final CourseService courseService;

    @Tool("搜索校园课程，按分类筛选，返回课程列表（标题、讲师、分类、浏览量和章节数）")
    public String searchCourses(
            @P("课程分类，如：编程开发、数据分析、前端开发、后端开发等，可选") String category,
            @P("页码，从1开始，默认1") int page,
            @P("每页数量，默认10") int size) {
        try {
            Page<CourseVO> result = courseService.listCourses(
                    isNotBlank(category) ? category : null, page, size, null);

            if (result.getRecords().isEmpty()) {
                return "没有找到符合条件的课程";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("共找到 ").append(result.getTotal()).append(" 门课程：\n");
            int limit = Math.min(result.getRecords().size(), 5);
            for (int i = 0; i < limit; i++) {
                CourseVO c = result.getRecords().get(i);
                sb.append(i + 1).append(". ").append(c.getTitle())
                        .append(" — ").append(c.getInstructor() != null ? c.getInstructor() : "未知讲师")
                        .append(" — ").append(c.getCategory() != null ? c.getCategory() : "未分类");
                if (c.getViewCount() != null) {
                    sb.append(" — ").append(c.getViewCount()).append(" 次浏览");
                }
                if (c.getChapterCount() != null && c.getChapterCount() > 0) {
                    sb.append(" — ").append(c.getChapterCount()).append(" 章节");
                }
                sb.append("\n");
            }
            if (result.getTotal() > limit) {
                sb.append("……还有 ").append(result.getTotal() - limit).append(" 门课程\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询课程时出错：" + e.getMessage();
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
