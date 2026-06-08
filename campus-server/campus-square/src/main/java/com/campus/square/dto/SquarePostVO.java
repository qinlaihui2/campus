package com.campus.square.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SquarePostVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String title;
    private String question;
    private String answer;
    private String referencesJson;
    private String category;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean liked;
    private LocalDateTime createdAt;
}
