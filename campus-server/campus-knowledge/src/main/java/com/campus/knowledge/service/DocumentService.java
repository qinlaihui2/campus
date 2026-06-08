package com.campus.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.ResultCode;
import com.campus.knowledge.entity.Document;
import com.campus.knowledge.mapper.DocumentMapper;
import com.campus.rag.embedding.EmbeddingService;
import com.campus.rag.splitter.DocumentParserService;
import com.campus.rag.splitter.DocumentSplitterService;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService extends ServiceImpl<DocumentMapper, Document> {

    private final DocumentSplitterService splitterService;
    private final EmbeddingService embeddingService;
    private final DocumentParserService parserService;

    /**
     * 异步处理文档入库（由 RabbitMQ 消费者调用）
     */
    @Transactional
    public void processDocument(Long documentId, InputStream inputStream) {
        Document document = this.getById(documentId);
        if (document == null) {
            log.error("文档不存在: {}", documentId);
            return;
        }

        try {
            document.setParseStatus("PARSING");
            this.updateById(document);

            // 1. 解析文档
            dev.langchain4j.data.document.Document parsed = parserService.parse(inputStream, document.getFileType());

            // 2. 分割文本
            List<TextSegment> segments = splitterService.split(parsed);

            // 3. 向量化并存储
            Map<String, Object> metadata = Map.of(
                    "document_id", documentId,
                    "knowledge_base_id", document.getKnowledgeBaseId(),
                    "title", document.getTitle()
            );
            int chunkCount = embeddingService.embedAndStore(segments, metadata);

            // 4. 更新文档状态
            document.setChunkCount(chunkCount);
            document.setParseStatus("COMPLETED");
            this.updateById(document);

            log.info("文档处理完成: {}, 切片数: {}", document.getTitle(), chunkCount);
        } catch (Exception e) {
            log.error("文档处理失败: {}", document.getTitle(), e);
            document.setParseStatus("FAILED");
            document.setParseError(e.getMessage());
            this.updateById(document);
        }
    }

    /**
     * 删除文档及其向量数据
     */
    @Transactional
    public void deleteDocumentWithVectors(Long documentId) {
        Document document = this.getById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }
        embeddingService.removeByDocumentId(documentId);
        this.removeById(documentId);
    }

    /**
     * 分页查询知识库下的文档
     */
    public Page<Document> getDocumentsByKnowledgeBase(Long knowledgeBaseId, int page, int size) {
        Page<Document> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(Document::getCreatedAt);
        return this.page(pageParam, wrapper);
    }
}
