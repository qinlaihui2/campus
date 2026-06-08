package com.campus.file.service;

import cn.hutool.core.util.IdUtil;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.ResultCode;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${minio.bucket}")
    private String bucket;

    public static final String DOCUMENT_PROCESS_QUEUE = "document.process.queue";

    /**
     * 上传文件到 MinIO
     */
    public String upload(MultipartFile file) {
        try {
            String objectName = IdUtil.fastSimpleUUID() + "_" + file.getOriginalFilename();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("文件上传成功: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 从 MinIO 下载文件
     */
    public InputStream download(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("文件下载失败: {}", objectName, e);
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
    }

    /**
     * 发送文档处理消息到 RabbitMQ
     */
    public void sendDocumentProcessMessage(Long documentId) {
        rabbitTemplate.convertAndSend(DOCUMENT_PROCESS_QUEUE, documentId);
        log.info("文档处理消息已发送: documentId={}", documentId);
    }
}
