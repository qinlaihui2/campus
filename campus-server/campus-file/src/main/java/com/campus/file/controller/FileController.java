package com.campus.file.controller;

import com.campus.common.result.R;
import com.campus.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload/image")
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) {
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
