package com.campus.rag.llm;

import com.campus.rag.retriever.RetrievalResult;
import com.campus.rag.retriever.RetrieverService;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final RetrieverService retrieverService;
    private final OpenAiStreamingChatModel streamingChatModel;

    private static final String SYSTEM_PROMPT = """
            你是"小园"，校圈平台的 AI 助手，就像一位在这里工作多年的辅导员，对学校的规章制度、办事流程、课程信息和校园生活了如指掌。

            回答规则：
            1. 直接回答问题，不要提"参考资料"、"知识库"、"查到"、"未找到"等字眼——你就是知道这些信息，不需要解释信息来源
            2. 如果知识库有相关内容，自然地融入回答中；如果没有，就用你自身的知识直接回答
            3. 使用简洁清晰的排版：用数字序号或小标题分段，重点加粗即可。禁止使用 --- 分隔线、代码块、表格、emoji 表情符号
            4. 语气友好耐心，像学长学姐在聊天，不要过于正式
            5. 涉及政策、规定类问题时，末尾可附一句温馨提示
            """;

    /**
     * 执行 RAG 检索并构建 Prompt
     */
    public String buildPrompt(String userQuestion, String conversationHistory, List<RetrievalResult> retrievalResults) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT).append("\n\n");

        if (retrievalResults != null && !retrievalResults.isEmpty()) {
            prompt.append("【以下内容来自学校官方资料，请直接引用回答，不要提及根据资料等字眼】\n");
            for (int i = 0; i < retrievalResults.size(); i++) {
                RetrievalResult result = retrievalResults.get(i);
                prompt.append(String.format("%s\n\n", result.getContent()));
            }
        }

        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            prompt.append("【对话历史】\n").append(conversationHistory).append("\n\n");
        }
        prompt.append("【用户问题】\n").append(userQuestion);

        return prompt.toString();
    }

    /**
     * 检索相关文档
     */
    public List<RetrievalResult> retrieve(String query) {
        return retrieverService.retrieve(query);
    }

    /**
     * 获取流式聊天模型（供对话模块使用）
     */
    public OpenAiStreamingChatModel getStreamingChatModel() {
        return streamingChatModel;
    }
}
