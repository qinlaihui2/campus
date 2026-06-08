package com.campus.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import com.campus.file.service.FileService;
import com.campus.knowledge.entity.Document;
import com.campus.knowledge.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final FileService fileService;

    /**
     * 上传文档到知识库
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Document> upload(@RequestParam Long knowledgeBaseId,
                              @RequestParam("file") MultipartFile file) throws IOException {
        // 1. 上传到 MinIO
        String fileUrl = fileService.upload(file);

        // 2. 创建文档记录
        Document document = new Document();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(file.getOriginalFilename());
        document.setFileName(file.getOriginalFilename());
        document.setFileType(getFileType(file.getOriginalFilename()));
        document.setFileSize(file.getSize());
        document.setFileUrl(fileUrl);
        document.setChunkCount(0);
        document.setParseStatus("PENDING");
        document.setIsEnabled(1);
        document.setCreatedBy(UserContext.getUserId());
        documentService.save(document);

        // 3. 发送异步处理消息
        fileService.sendDocumentProcessMessage(document.getId());

        return R.ok("上传成功，正在后台解析", document);
    }

    @GetMapping("/list/{knowledgeBaseId}")
    public R<Page<Document>> list(@PathVariable Long knowledgeBaseId,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return R.ok(documentService.getDocumentsByKnowledgeBase(knowledgeBaseId, page, size));
    }

    @GetMapping("/{id}")
    public R<Document> detail(@PathVariable Long id) {
        return R.ok(documentService.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<String> delete(@PathVariable Long id) {
        documentService.deleteDocumentWithVectors(id);
        return R.ok("删除成功");
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public R<String> toggleStatus(@PathVariable Long id, @RequestParam Integer enabled) {
        Document document = documentService.getById(id);
        document.setIsEnabled(enabled);
        documentService.updateById(document);
        return R.ok("更新成功");
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "txt";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".docx")) return "docx";
        if (lower.endsWith(".md")) return "md";
        if (lower.endsWith(".txt")) return "txt";
        return "txt";
    }
}
