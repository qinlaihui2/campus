package com.campus.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/** 接口限流注解，基于 Redisson RRateLimiter */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流 key 前缀 */
    String key() default "rate_limit";

    /** 每秒允许的请求数 */
    long permitsPerSecond() default 10;

    /** 获取许可的超时时间 */
    long timeout() default 1;

    /** 超时时间单位 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /** 限流提示 */
    String message() default "请求过于频繁，请稍后再试";
}
