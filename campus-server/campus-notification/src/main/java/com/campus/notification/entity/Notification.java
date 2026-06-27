package com.campus.notification.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
public class Notification extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    private String type;

    private String title;

    private String content;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("is_read")
    private Boolean isRead;
}
