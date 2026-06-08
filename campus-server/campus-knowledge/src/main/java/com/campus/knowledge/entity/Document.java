package com.campus.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document")
public class Document extends BaseEntity {

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    private String title;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_url")
    private String fileUrl;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("parse_status")
    private String parseStatus;

    @TableField("parse_error")
    private String parseError;

    @TableField("is_enabled")
    private Integer isEnabled;

    @TableField("created_by")
    private Long createdBy;
}
