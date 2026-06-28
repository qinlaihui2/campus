package com.campus.agent.tool;

import com.campus.rag.retriever.RetrievalResult;
import com.campus.rag.retriever.RetrieverService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RagTool {

    private final RetrieverService retrieverService;

    @Tool("搜索校园知识库，查询学校规章制度、办事流程、政策等官方信息。当用户问怎么办、流程、规定、政策时调用")
    public String searchKnowledge(@P("用户的问题或关键词") String query) {
        try {
            List<RetrievalResult> results = retrieverService.retrieve(query);
            String text = formatResults(results);
            ToolCallRecorder.record("searchKnowledge", "关键词=" + query,
                    text.length() > 200 ? text.substring(0, 200) + "..." : text);
            return text;
        } catch (Exception e) {
            return "搜索知识库时出错：" + e.getMessage();
        }
    }

    private String formatResults(List<RetrievalResult> results) {
        if (results.isEmpty()) return "知识库中没有找到相关信息";
        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(results.size()).append(" 条相关知识：\n\n");
        for (int i = 0; i < results.size(); i++) {
            sb.append(results.get(i).getContent()).append("\n\n");
        }
        return sb.toString();
    }
}
