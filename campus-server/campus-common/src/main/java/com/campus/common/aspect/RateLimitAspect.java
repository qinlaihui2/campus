package com.campus.common.aspect;

import com.campus.common.annotation.RateLimit;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.ResultCode;
import com.campus.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();

        // key: rate_limit:类名:方法名:userId
        Long userId = getUserId();
        String key = rateLimit.key() + ":" + signature.getDeclaringType().getSimpleName()
                + ":" + methodName + ":" + (userId != null ? userId : "anon");

        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        limiter.trySetRate(RateType.OVERALL,
                rateLimit.permitsPerSecond(),
                1,
                RateIntervalUnit.SECONDS);

        if (limiter.tryAcquire(rateLimit.timeout(), rateLimit.timeUnit())) {
            return joinPoint.proceed();
        }

        log.warn("接口限流触发: key={}", key);
        throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(),
                rateLimit.message());
    }

    private Long getUserId() {
        try {
            return UserContext.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
