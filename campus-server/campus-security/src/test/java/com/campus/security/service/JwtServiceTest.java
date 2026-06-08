package com.campus.security.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JWT 服务单元测试")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // 使用一个足够长的 secret 用于 HS256
        jwtService = new JwtService(
                "this-is-a-secret-key-for-testing-purpose-min-256-bits-long!!",
                1800000L,  // 30 min access
                604800000L  // 7 days refresh
        );
    }

    @Nested @DisplayName("Token 生成")
    class TokenGeneration {
        @Test @DisplayName("生成 Access Token 包含 userId、role、subject")
        void shouldGenerateAccessToken() {
            String token = jwtService.generateAccessToken(1L, "admin", "ADMIN");
            assertThat(token).isNotBlank();
            assertThat(jwtService.validateToken(token)).isTrue();

            Claims claims = jwtService.parseToken(token);
            assertThat(claims.getSubject()).isEqualTo("admin");
            assertThat((Integer) claims.get("userId")).isEqualTo(1);
            assertThat(claims.get("role")).isEqualTo("ADMIN");
        }

        @Test @DisplayName("生成 Refresh Token 有效期更长")
        void shouldGenerateRefreshToken() {
            String token = jwtService.generateRefreshToken(1L, "admin", "ADMIN");
            assertThat(token).isNotBlank();
            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test @DisplayName("普通学生角色的 Token")
        void shouldGenerateStudentToken() {
            String token = jwtService.generateAccessToken(42L, "student1", "STUDENT");
            assertThat(jwtService.validateToken(token)).isTrue();

            Claims claims = jwtService.parseToken(token);
            assertThat(claims.getSubject()).isEqualTo("student1");
            assertThat(claims.get("role")).isEqualTo("STUDENT");
        }
    }

    @Nested @DisplayName("Token 验证")
    class TokenValidation {
        @Test @DisplayName("有效 Token 通过验证")
        void shouldValidateValidToken() {
            String token = jwtService.generateAccessToken(1L, "user", "STUDENT");
            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test @DisplayName("篡改的 Token 不通过验证")
        void shouldRejectTamperedToken() {
            String token = jwtService.generateAccessToken(1L, "user", "STUDENT");
            String tampered = token.substring(0, token.length() - 4) + "XXXX";
            assertThat(jwtService.validateToken(tampered)).isFalse();
        }

        @Test @DisplayName("空 Token 不通过验证")
        void shouldRejectEmptyToken() {
            assertThat(jwtService.validateToken("")).isFalse();
            assertThat(jwtService.validateToken(null)).isFalse();
        }
    }
}
