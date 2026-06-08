package com.campus.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
}
