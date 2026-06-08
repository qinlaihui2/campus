package com.campus.rag.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 将文本片段向量化并存储
     */
    public int embedAndStore(List<TextSegment> segments, Map<String, Object> sharedMetadata) {
        int count = 0;
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            if (sharedMetadata != null) {
                sharedMetadata.forEach((k, v) -> segment.metadata().put(k, String.valueOf(v)));
            }
            segment.metadata().put("chunk_index", String.valueOf(i));

            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
            count++;
        }
        log.info("向量化完成: {} 个文本块", count);
        return count;
    }

    /**
     * 删除指定文档的所有向量
     */
    public void removeByDocumentId(Long documentId) {
        embeddingStore.removeAll(
                dev.langchain4j.store.embedding.filter.MetadataFilterBuilder
                        .metadataKey("document_id")
                        .isEqualTo(documentId.toString()));
    }
}
