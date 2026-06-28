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

    @Tool("搜索校园广场的问答帖子，按分类和关键词筛选，返回帖子标题、作者、浏览量")
    public String searchSquarePosts(
            @P("帖子分类，可选") String category,
            @P("关键词搜索，可选") String keyword) {
        try {
            Page<SquarePostVO> result = squarePostService.listPosts(
                    isNotBlank(category) ? category : null,
                    isNotBlank(keyword) ? keyword : null,
                    1, 10, null);

            if (result.getRecords().isEmpty()) {
                return "没有找到相关的广场帖子";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("共找到 ").append(result.getTotal()).append(" 条广场帖子：\n");
            int limit = Math.min(result.getRecords().size(), 5);
            for (int i = 0; i < limit; i++) {
                SquarePostVO p = result.getRecords().get(i);
                sb.append(i + 1).append(". ").append(p.getTitle())
                        .append(" — ").append(p.getUserNickname() != null ? p.getUserNickname() : "匿名")
                        .append(" — ").append(p.getCategory() != null ? p.getCategory() : "未分类");
                if (p.getViewCount() != null) {
                    sb.append(" — ").append(p.getViewCount()).append(" 次浏览");
                }
                sb.append("\n");
            }
            if (result.getTotal() > limit) {
                sb.append("……还有 ").append(result.getTotal() - limit).append(" 条帖子\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询广场帖子时出错：" + e.getMessage();
        }
    }

    @Tool("获取校园广场当前的热门帖子")
    public String getHotPosts() {
        try {
            Page<SquarePostVO> result = squarePostService.listHot(1, 10, null);

            if (result.getRecords().isEmpty()) {
                return "目前没有热门帖子";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("当前热门帖子 Top").append(Math.min(result.getRecords().size(), 5)).append("：\n");
            int limit = Math.min(result.getRecords().size(), 5);
            for (int i = 0; i < limit; i++) {
                SquarePostVO p = result.getRecords().get(i);
                sb.append(i + 1).append(". ").append(p.getTitle())
                        .append(" — ").append(p.getUserNickname() != null ? p.getUserNickname() : "匿名")
                        .append(" — ").append(p.getLikeCount() != null ? p.getLikeCount() : 0).append(" 赞")
                        .append(" — ").append(p.getViewCount() != null ? p.getViewCount() : 0).append(" 次浏览")
                        .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "获取热门帖子时出错：" + e.getMessage();
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
