package com.campus.agent.tool;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.announcement.entity.Announcement;
import com.campus.announcement.service.AnnouncementService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnnouncementTool {

    private final AnnouncementService announcementService;

    @Tool("搜索校园公告通知。当用户问通知、公告、放假、考试安排时调用")
    public String searchAnnouncements(
            @P("公告分类如教务通知、校园活动等，不指定传空") String category) {
        try {
            Page<Announcement> result = announcementService.listByCategory(
                    isNotBlank(category) ? category : null, 1, 10);
            String text = formatAnnouncements(result);
            ToolCallRecorder.record("searchAnnouncements", "分类=" + category,
                    text.length() > 200 ? text.substring(0, 200) + "..." : text);
            return text;
        } catch (Exception e) {
            return "查询公告时出错：" + e.getMessage();
        }
    }

    private String formatAnnouncements(Page<Announcement> result) {
        if (result.getRecords().isEmpty()) return "没有找到相关公告";
        StringBuilder sb = new StringBuilder();
        sb.append("共找到 ").append(result.getTotal()).append(" 条公告：\n");
        int limit = Math.min(result.getRecords().size(), 5);
        for (int i = 0; i < limit; i++) {
            Announcement a = result.getRecords().get(i);
            sb.append(i + 1).append(". ").append(a.getTitle());
            if (a.getSummary() != null && !a.getSummary().isBlank())
                sb.append(" — ").append(a.getSummary());
            if (a.getCategory() != null) sb.append(" 【").append(a.getCategory()).append("】");
            sb.append("\n");
        }
        if (result.getTotal() > limit) sb.append("……还有 ").append(result.getTotal() - limit).append(" 条\n");
        return sb.toString();
    }

    private boolean isNotBlank(String s) { return s != null && !s.isBlank(); }
}
