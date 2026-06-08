package com.campus.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    PARAM_ERROR(400, "参数错误"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    ACCOUNT_LOCKED(1004, "账户已被锁定，请15分钟后重试"),
    VERIFY_CODE_ERROR(1005, "验证码错误"),
    VERIFY_CODE_EXPIRED(1006, "验证码已过期"),
    TOKEN_EXPIRED(1007, "Token已过期"),
    TOKEN_INVALID(1008, "Token无效"),

    // 文件模块 2xxx
    FILE_UPLOAD_FAILED(2001, "文件上传失败"),
    FILE_NOT_FOUND(2002, "文件不存在"),
    FILE_FORMAT_UNSUPPORTED(2003, "不支持的文件格式"),
    FILE_SIZE_EXCEEDED(2004, "文件大小超出限制"),

    // 知识库模块 3xxx
    KNOWLEDGE_BASE_NOT_FOUND(3001, "知识库不存在"),
    DOCUMENT_NOT_FOUND(3002, "文档不存在"),
    DOCUMENT_PARSE_FAILED(3003, "文档解析失败"),

    // 对话模块 4xxx
    CONVERSATION_NOT_FOUND(4001, "会话不存在"),
    AI_SERVICE_ERROR(4002, "AI服务异常，请稍后重试"),
    RAG_RETRIEVAL_FAILED(4003, "知识检索失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
