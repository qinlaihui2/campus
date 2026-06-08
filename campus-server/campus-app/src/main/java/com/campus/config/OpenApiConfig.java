package com.campus.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI campusOpenAPI() {
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("在下方输入 JWT Token（登录接口返回的 accessToken）");

        return new OpenAPI()
                .info(new Info()
                        .title("校圈 CampusHub API")
                        .description("基于 RAG 技术的 AI 校园综合平台接口文档\n\n"
                                + "**14 个模块**: AI对话 · 课程学习 · 问答广场 · 知识库 · 私信 · 失物招领 · 公告 · 反馈 · 个人中心")
                        .version("1.0.0")
                        .contact(new Contact().name("CampusHub").email("admin@campushub.dev"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .schemaRequirement("BearerAuth", jwtScheme);
    }
}
