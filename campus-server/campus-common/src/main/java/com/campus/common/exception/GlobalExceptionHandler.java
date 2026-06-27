package com.campus.common.exception;

import com.campus.common.filter.TraceIdFilter;
import com.campus.common.result.R;
import com.campus.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 获取当前请求的 traceId */
    private String traceId() {
        return MDC.get(TraceIdFilter.TRACE_ID_KEY);
    }

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("[{}] 业务异常: code={}, message={}", traceId(), e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("[{}] 认证失败: {}", traceId(), e.getMessage());
        return R.fail(ResultCode.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("[{}] 权限不足: {}", traceId(), e.getMessage());
        return R.fail(ResultCode.FORBIDDEN);
    }

    /** 请求体 JSON 格式错误 / 类型不匹配 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[{}] 请求体解析失败: {}", traceId(), e.getMessage());
        return R.fail(ResultCode.PARAM_ERROR.getCode(), "请求体格式错误，请检查 JSON 结构");
    }

    /** URL 参数类型不匹配 (如 /api/items/abc) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[{}] 参数类型不匹配: {} = {}", traceId(), e.getName(), e.getValue());
        return R.fail(ResultCode.PARAM_ERROR.getCode(),
                String.format("参数 '%s' 类型错误，期望 %s", e.getName(),
                        e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "数字"));
    }

    /** 缺少必需的请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("[{}] 缺少必需参数: {}", traceId(), e.getParameterName());
        return R.fail(ResultCode.PARAM_ERROR.getCode(),
                "缺少必需参数: " + e.getParameterName());
    }

    /** HTTP 方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[{}] 不支持的请求方法: {}", traceId(), e.getMethod());
        return R.fail(ResultCode.PARAM_ERROR.getCode(),
                "不支持的请求方式: " + e.getMethod() + "，请使用 " + String.join(", ", e.getSupportedMethods()));
    }

    /** @Validated 在 Controller 层触发的校验异常 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValidationException(Exception e) {
        String message = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().hasFieldErrors()) {
            message = ex.getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof BindException ex && ex.getBindingResult().hasFieldErrors()) {
            message = ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        log.warn("[{}] 参数校验失败: {}", traceId(), message);
        return R.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /** Service 层 @Validated 触发的约束校验 */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("[{}] 约束校验失败: {}", traceId(), message);
        return R.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("[{}] 上传文件过大: {}", traceId(), e.getMessage());
        return R.fail(ResultCode.FILE_SIZE_EXCEEDED);
    }

    /** 兜底：未预期的系统异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        String traceId = traceId();
        log.error("[{}] 系统异常", traceId, e);
        // 生产环境不应暴露详细异常信息给前端，但可返回 traceId 方便排查
        return R.fail(ResultCode.FAIL.getCode(),
                "系统内部错误 (traceId: " + traceId + ")");
    }
}
