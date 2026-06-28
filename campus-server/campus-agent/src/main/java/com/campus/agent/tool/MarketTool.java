package com.campus.agent.tool;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.dto.MarketItemVO;
import com.campus.market.service.MarketService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketTool {

    private final MarketService marketService;

    @Tool("搜索二手市场商品。当用户询问买、卖、二手、价格相关内容时调用此工具")
    public String searchItems(
            @P("商品分类如电子数码、书籍教材等，用户没指定就传空字符串") String category,
            @P("成色NEW/LIKE_NEW/GOOD/ACCEPTABLE，不指定传空") String condition,
            @P("关键词，用户提到什么商品就传什么，空字符串表示不限制") String keyword,
            @P("最低价格，不限制传空") Double minPrice,
            @P("最高价格，不限制传空") Double maxPrice,
            @P("排序newest/price_asc/price_desc") String sort) {
        try {
            Page<MarketItemVO> result = marketService.listItems(
                    isNotBlank(category) ? category : null,
                    isNotBlank(condition) ? condition : null,
                    isNotBlank(keyword) ? keyword : null,
                    minPrice, maxPrice, isNotBlank(sort) ? sort : "newest",
                    1, 10, null);
            String text = formatItems(result);
            ToolCallRecorder.record("searchItems",
                    "分类=" + category + " 关键词=" + keyword,
                    text.length() > 200 ? text.substring(0, 200) + "..." : text);
            return text;
        } catch (Exception e) {
            return "查询二手商品时出错：" + e.getMessage();
        }
    }

    private String formatItems(Page<MarketItemVO> result) {
        if (result.getRecords().isEmpty()) return "没有找到符合条件的二手商品";
        StringBuilder sb = new StringBuilder();
        sb.append("共找到 ").append(result.getTotal()).append(" 件商品：\n");
        int limit = Math.min(result.getRecords().size(), 5);
        for (int i = 0; i < limit; i++) {
            MarketItemVO item = result.getRecords().get(i);
            sb.append(i + 1).append(". ").append(item.getTitle())
                    .append(" — ¥").append(item.getPrice())
                    .append(" — ").append(item.getCondition() != null ? item.getCondition() : "未知")
                    .append(" — ").append(item.getCategory() != null ? item.getCategory() : "");
            sb.append("\n");
        }
        if (result.getTotal() > limit) sb.append("……还有 ").append(result.getTotal() - limit).append(" 件\n");
        return sb.toString();
    }

    private boolean isNotBlank(String s) { return s != null && !s.isBlank(); }
}
