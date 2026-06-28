package com.campus.agent.tool;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.dto.LostFoundPostVO;
import com.campus.lostfound.service.LostFoundPostService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LostFoundTool {

    private final LostFoundPostService lostFoundPostService;

    @Tool("搜索失物招领信息，按类型（丢失/捡到）、分类和关键词筛选，返回标题、地点、类型")
    public String searchLostFound(
            @P("类型：LOST=丢失物品, FOUND=捡到物品") String type,
            @P("物品分类，如：证件、电子产品、生活用品等，可选") String category,
            @P("关键词搜索，可选") String keyword) {
        try {
            Page<LostFoundPostVO> result = lostFoundPostService.listPosts(type, category, keyword, 1, 10);

            if (result.getRecords().isEmpty()) {
                return "没有找到相关的失物招领信息";
            }

            StringBuilder sb = new StringBuilder();
            String typeLabel = "FOUND".equals(type) ? "捡到" : "丢失";
            sb.append("共找到 ").append(result.getTotal()).append(" 条").append(typeLabel).append("物品信息：\n");
            int limit = Math.min(result.getRecords().size(), 5);
            for (int i = 0; i < limit; i++) {
                LostFoundPostVO p = result.getRecords().get(i);
                sb.append(i + 1).append(". 【").append("LOST".equals(p.getType()) ? "丢失" : "捡到").append("】")
                        .append(p.getTitle())
                        .append(" — ").append(p.getLocation() != null ? p.getLocation() : "未知地点")
                        .append(" — ").append(p.getCategory() != null ? p.getCategory() : "未分类")
                        .append("\n");
            }
            if (result.getTotal() > limit) {
                sb.append("……还有 ").append(result.getTotal() - limit).append(" 条记录\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询失物招领时出错：" + e.getMessage();
        }
    }
}
