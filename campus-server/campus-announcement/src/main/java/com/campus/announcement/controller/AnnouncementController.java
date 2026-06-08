package com.campus.announcement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.announcement.entity.Announcement;
import com.campus.announcement.entity.AnnouncementAttachment;
import com.campus.announcement.service.AnnouncementService;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.PageResult;
import com.campus.common.result.R;
import com.campus.common.result.ResultCode;
import com.campus.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final FileService fileService;

    @GetMapping
    public R<PageResult<Announcement>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Announcement> result = announcementService.listByCategory(category, page, size);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/carousel")
    public R<List<Announcement>> carousel() {
        return R.ok(announcementService.listCarousel());
    }

    @GetMapping("/{id}")
    public R<Announcement> detail(@PathVariable Long id) {
        return R.ok(announcementService.getDetail(id));
    }

    @GetMapping("/{id}/attachments")
    public R<List<AnnouncementAttachment>> attachments(@PathVariable Long id) {
        return R.ok(announcementService.getAttachments(id));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable Long attachmentId) {
        AnnouncementAttachment att = announcementService.getAttachmentById(attachmentId);
        if (att == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        InputStream inputStream = fileService.download(att.getFileUrl());
        String encodedFileName = URLEncoder.encode(att.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }
}
