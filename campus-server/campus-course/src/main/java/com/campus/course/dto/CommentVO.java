package com.campus.course.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private Long replyToUserId;
    private String replyToUserNickname;
    private String content;
    private Integer likeCount;
    private Boolean liked;
    private LocalDateTime createdAt;
    private List<CommentVO> replies;
}
