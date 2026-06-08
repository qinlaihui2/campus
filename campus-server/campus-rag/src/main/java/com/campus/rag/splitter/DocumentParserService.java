package com.campus.rag.splitter;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParserService {

    /**
     * 根据文件类型解析文档
     */
    public Document parse(InputStream inputStream, String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> {
                ApachePdfBoxDocumentParser parser = new ApachePdfBoxDocumentParser();
                yield parser.parse(inputStream);
            }
            case "txt", "md" -> {
                TextDocumentParser parser = new TextDocumentParser();
                yield parser.parse(inputStream);
            }
            case "docx" -> {
                // DOCX 使用 TextDocumentParser 配合 Tika 或直接解析
                TextDocumentParser parser = new TextDocumentParser();
                yield parser.parse(inputStream);
            }
            default -> throw new IllegalArgumentException("不支持的文件类型: " + fileType);
        };
    }
}
