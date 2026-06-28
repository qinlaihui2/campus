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

    @Tool("搜索校园知识库，获取学校规章制度、办事流程、校园生活等官方信息。当用户询问学校政策、规定、流程时必须使用此工具")
    public String searchKnowledge(
            @P("搜索关键词，如：奖学金申请条件、校园卡挂失流程、图书馆开放时间等") String query) {
        try {
            List<RetrievalResult> results = retrieverService.retrieve(query);

            if (results.isEmpty()) {
                return "知识库中没有找到相关信息";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("共检索到 ").append(results.size()).append(" 条相关知识：\n\n");
            for (int i = 0; i < results.size(); i++) {
                RetrievalResult r = results.get(i);
                sb.append("【").append(i + 1).append("】").append(r.getContent()).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "搜索知识库时出错：" + e.getMessage();
        }
    }
}
