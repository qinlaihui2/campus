package com.campus.my.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MyItemVO {
    /** 该条记录的 ID（点赞记录ID或收藏记录ID） */
    private Long id;
    /** 类型: course / square_post */
    private String type;
    /** 目标实体 ID */
    private Long targetId;
    /** 标题 */
    private String title;
    /** 描述/摘要 */
    private String description;
    /** 封面图 */
    private String coverImage;
    /** 点赞数 */
    private Integer likeCount;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
