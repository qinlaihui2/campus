package com.campus.knowledge.consumer;

import com.campus.file.service.FileService;
import com.campus.knowledge.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessConsumer {

    private final DocumentService documentService;
    private final FileService fileService;

    @RabbitListener(queues = "document.process.queue")
    public void handleDocumentProcess(Long documentId) {
        log.info("收到文档处理消息: documentId={}", documentId);
        try {
            // 从 MinIO 下载文件流
            com.campus.knowledge.entity.Document doc = documentService.getById(documentId);
            if (doc == null || doc.getFileUrl() == null) {
                log.error("文档或文件URL不存在: {}", documentId);
                return;
            }
            InputStream inputStream = fileService.download(doc.getFileUrl());
            documentService.processDocument(documentId, inputStream);
        } catch (Exception e) {
            log.error("文档处理异常: documentId={}", documentId, e);
        }
    }
}
