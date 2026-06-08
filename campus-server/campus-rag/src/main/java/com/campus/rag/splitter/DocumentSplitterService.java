package com.campus.rag.splitter;

import com.campus.rag.config.RagProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentSplitterService {

    private final RagProperties ragProperties;

    /**
     * 将文档按段落分割为指定大小的文本块
     */
    public List<TextSegment> split(Document document) {
        DocumentSplitter splitter = DocumentSplitters.recursive(
                ragProperties.getChunkSize(),
                ragProperties.getChunkOverlap()
        );
        return splitter.split(document);
    }

    /**
     * 将纯文本按指定大小分割
     */
    public List<TextSegment> splitText(String text) {
        Document document = Document.from(text);
        return split(document);
    }
}
