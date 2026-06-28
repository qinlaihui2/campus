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

    @Tool("搜索校园公告通知，按分类筛选，返回公告标题、摘要、分类、发布时间")
    public String searchAnnouncements(
            @P("公告分类，如：教务通知、校园活动、放假通知等，可选，不传则查全部") String category) {
        try {
            Page<Announcement> result = announcementService.listByCategory(
                    isNotBlank(category) ? category : null, 1, 10);

            if (result.getRecords().isEmpty()) {
                return "没有找到相关的公告";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("共找到 ").append(result.getTotal()).append(" 条公告：\n");
            int limit = Math.min(result.getRecords().size(), 5);
            for (int i = 0; i < limit; i++) {
                Announcement a = result.getRecords().get(i);
                sb.append(i + 1).append(". ").append(a.getTitle());
                if (a.getSummary() != null && !a.getSummary().isBlank()) {
                    sb.append(" — ").append(a.getSummary());
                }
                if (a.getCategory() != null) {
                    sb.append(" — 【").append(a.getCategory()).append("】");
                }
                if (a.getPublishedAt() != null) {
                    sb.append(" — ").append(a.getPublishedAt());
                }
                sb.append("\n");
            }
            if (result.getTotal() > limit) {
                sb.append("……还有 ").append(result.getTotal() - limit).append(" 条公告\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询公告时出错：" + e.getMessage();
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
