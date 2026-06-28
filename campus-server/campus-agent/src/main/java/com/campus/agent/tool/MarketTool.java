package com.campus.agent.tool;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.dto.MarketItemVO;
import com.campus.market.service.MarketService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class MarketTool {

    private final MarketService marketService;

    @Tool("搜索二手交易商品，支持按分类、成色、关键词、价格范围筛选，返回商品标题、价格、成色、分类")
    public String searchItems(
            @P("商品分类，如：电子数码、书籍教材、生活用品等，可选") String category,
            @P("商品成色：NEW=全新, LIKE_NEW=几乎全新, GOOD=良好, ACCEPTABLE=一般，可选") String condition,
            @P("关键词搜索，可选") String keyword,
            @P("最低价格，可选") Double minPrice,
            @P("最高价格，可选") Double maxPrice,
            @P("排序方式：newest=最新, price_asc=价格升序, price_desc=价格降序，默认newest") String sort) {
        try {
            Page<MarketItemVO> result = marketService.listItems(
                    isNotBlank(category) ? category : null,
                    isNotBlank(condition) ? condition : null,
                    isNotBlank(keyword) ? keyword : null,
                    minPrice, maxPrice,
                    isNotBlank(sort) ? sort : "newest",
                    1, 10, null);

            if (result.getRecords().isEmpty()) {
                return "没有找到符合条件的二手商品";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("共找到 ").append(result.getTotal()).append(" 件商品：\n");
            int limit = Math.min(result.getRecords().size(), 5);
            for (int i = 0; i < limit; i++) {
                MarketItemVO item = result.getRecords().get(i);
                sb.append(i + 1).append(". ").append(item.getTitle())
                        .append(" — ¥").append(item.getPrice())
                        .append(" — ").append(item.getCondition() != null ? item.getCondition() : "未知");
                if (item.getCategory() != null) {
                    sb.append(" — ").append(item.getCategory());
                }
                sb.append("\n");
            }
            if (result.getTotal() > limit) {
                sb.append("……还有 ").append(result.getTotal() - limit).append(" 件商品\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询二手商品时出错：" + e.getMessage();
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
