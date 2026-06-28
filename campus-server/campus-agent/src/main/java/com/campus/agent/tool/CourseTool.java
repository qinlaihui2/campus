package com.campus.agent.tool;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.course.service.CourseService;
import com.campus.course.dto.CourseVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseTool {

    private final CourseService courseService;

    @Tool("搜索校园课程库。当用户询问课程、上课、学习相关内容时调用此工具")
    public String searchCourses(
            @P("课程分类如编程开发、数据分析等，用户没指定就传空字符串") String category,
            @P("页码默认1") int page,
            @P("每页数量默认10") int size) {
        try {
            Page<CourseVO> result = courseService.listCourses(
                    isNotBlank(category) ? category : null, page, size, null);
            String text = formatCourses(result);
            ToolCallRecorder.record("searchCourses",
                    "分类=" + category + " 页码=" + page,
                    text.length() > 200 ? text.substring(0, 200) + "..." : text);
            return text;
        } catch (Exception e) {
            return "查询课程时出错：" + e.getMessage();
        }
    }

    private String formatCourses(Page<CourseVO> result) {
        if (result.getRecords().isEmpty()) return "没有找到符合条件的课程";
        StringBuilder sb = new StringBuilder();
        sb.append("共找到 ").append(result.getTotal()).append(" 门课程：\n");
        int limit = Math.min(result.getRecords().size(), 5);
        for (int i = 0; i < limit; i++) {
            CourseVO c = result.getRecords().get(i);
            sb.append(i + 1).append(". ").append(c.getTitle())
                    .append(" — ").append(c.getInstructor() != null ? c.getInstructor() : "未知讲师")
                    .append(" — ").append(c.getCategory() != null ? c.getCategory() : "未分类");
            if (c.getChapterCount() != null && c.getChapterCount() > 0)
                sb.append(" — ").append(c.getChapterCount()).append(" 章节");
            sb.append("\n");
        }
        if (result.getTotal() > limit) sb.append("……还有 ").append(result.getTotal() - limit).append(" 门\n");
        return sb.toString();
    }

    private boolean isNotBlank(String s) { return s != null && !s.isBlank(); }
}
