package com.campus.agent.tool;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.square.dto.SquarePostVO;
import com.campus.square.service.SquarePostService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SquareTool {

    private final SquarePostService squarePostService;

    @Tool("搜索校园广场问答帖子。当用户问怎么办、流程、在哪、如何，以及讨论、话题时调用")
    public String searchSquarePosts(
            @P("帖子分类，不指定传空") String category,
            @P("用户问题中的关键词") String keyword) {
        try {
            Page<SquarePostVO> result = squarePostService.listPosts(
                    isNotBlank(category) ? category : null,
                    isNotBlank(keyword) ? keyword : null, 1, 10, null);
            String text = formatPosts(result);
            ToolCallRecorder.record("searchSquarePosts", "分类=" + category + " 关键词=" + keyword,
                    text.length() > 200 ? text.substring(0, 200) + "..." : text);
            return text;
        } catch (Exception e) {
            return "查询广场帖子时出错：" + e.getMessage();
        }
    }

    @Tool("获取广场热门帖子")
    public String getHotPosts() {
        try {
            Page<SquarePostVO> result = squarePostService.listHot(1, 10, null);
            String text = formatPosts(result);
            ToolCallRecorder.record("getHotPosts", "", text.length() > 200 ? text.substring(0, 200) + "..." : text);
            return text;
        } catch (Exception e) {
            return "获取热门帖子时出错：" + e.getMessage();
        }
    }

    private String formatPosts(Page<SquarePostVO> result) {
        if (result.getRecords().isEmpty()) return "目前没有相关帖子";
        StringBuilder sb = new StringBuilder();
        sb.append("共找到 ").append(result.getTotal()).append(" 条帖子：\n");
        int limit = Math.min(result.getRecords().size(), 5);
        for (int i = 0; i < limit; i++) {
            SquarePostVO p = result.getRecords().get(i);
            sb.append(i + 1).append(". ").append(p.getTitle())
                    .append(" — ").append(p.getUserNickname() != null ? p.getUserNickname() : "匿名")
                    .append(" — ").append(p.getLikeCount()).append(" 赞\n");
        }
        if (result.getTotal() > limit) sb.append("……还有 ").append(result.getTotal() - limit).append(" 条\n");
        return sb.toString();
    }

    private boolean isNotBlank(String s) { return s != null && !s.isBlank(); }
}
