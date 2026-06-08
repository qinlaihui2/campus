package com.campus.rag.retriever;

import com.campus.rag.config.RagProperties;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieverService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RagProperties ragProperties;

    /**
     * 混合检索：向量相似度检索
     */
    public List<RetrievalResult> retrieve(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                queryEmbedding, ragProperties.getTopK(), ragProperties.getSimilarityThreshold());

        return matches.stream()
                .map(match -> RetrievalResult.builder()
                        .content(match.embedded().text())
                        .score(match.score())
                        .metadata(match.embedded().metadata().toMap())
                        .build())
                .collect(Collectors.toList());
    }
}
