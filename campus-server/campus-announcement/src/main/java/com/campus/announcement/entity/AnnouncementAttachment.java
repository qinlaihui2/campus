package com.campus.announcement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("announcement_attachment")
public class AnnouncementAttachment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long announcementId;
    private String fileName;
    private Long fileSize;
    private String fileUrl;
    private LocalDateTime createdAt;
}
