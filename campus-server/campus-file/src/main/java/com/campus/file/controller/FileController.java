package com.campus.file.controller;

import com.campus.common.result.R;
import com.campus.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /** 支持的视频格式 */
    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime",
            "video/x-msvideo", "video/x-matroska"
    );

    @PostMapping("/upload/image")
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String objectName = fileService.upload(file);
        return R.ok(objectName);
    }

    @PostMapping("/upload/video")
    public R<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !VIDEO_TYPES.contains(contentType)) {
            return R.fail("不支持的视频格式，支持：MP4、WebM、MOV、AVI、MKV");
        }
        String objectName = fileService.upload(file);
        return R.ok(objectName);
    }

    @GetMapping("/files/{objectName}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.InputStreamResource> download(
            @PathVariable String objectName) {
        var inputStream = fileService.download(objectName);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(new org.springframework.core.io.InputStreamResource(inputStream));
    }
}
