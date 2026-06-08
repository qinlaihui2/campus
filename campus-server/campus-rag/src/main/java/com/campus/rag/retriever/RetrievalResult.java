package com.campus.rag.retriever;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RetrievalResult {

    private String content;
    private Double score;
    private Map<String, Object> metadata;
}
