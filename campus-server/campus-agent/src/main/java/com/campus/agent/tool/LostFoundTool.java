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

    @Tool("搜索失物招领。当用户说丢了东西、捡到东西、找东西时调用此工具")
    public String searchLostFound(
            @P("类型：LOST=丢失物品, FOUND=捡到物品。用户说丢了就传LOST，说捡到就传FOUND，不确定就传空") String type,
            @P("物品分类如证件、电子产品，不指定传空") String category,
            @P("用户提到的物品关键词") String keyword) {
        try {
            Page<LostFoundPostVO> result = lostFoundPostService.listPosts(type, category, keyword, 1, 10);
            String text = formatPosts(result);
            ToolCallRecorder.record("searchLostFound", "类型=" + type + " 关键词=" + keyword,
                    text.length() > 200 ? text.substring(0, 200) + "..." : text);
            return text;
        } catch (Exception e) {
            return "查询失物招领时出错：" + e.getMessage();
        }
    }

    private String formatPosts(Page<LostFoundPostVO> result) {
        if (result.getRecords().isEmpty()) return "没有找到相关的失物招领信息";
        StringBuilder sb = new StringBuilder();
        sb.append("共找到 ").append(result.getTotal()).append(" 条记录：\n");
        int limit = Math.min(result.getRecords().size(), 5);
        for (int i = 0; i < limit; i++) {
            LostFoundPostVO p = result.getRecords().get(i);
            sb.append(i + 1).append(". 【").append("LOST".equals(p.getType()) ? "丢失" : "捡到").append("】")
                    .append(p.getTitle()).append(" — ").append(p.getLocation() != null ? p.getLocation() : "未知");
            sb.append("\n");
        }
        if (result.getTotal() > limit) sb.append("……还有 ").append(result.getTotal() - limit).append(" 条\n");
        return sb.toString();
    }
}
