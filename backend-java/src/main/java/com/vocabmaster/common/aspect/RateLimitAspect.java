package com.vocabmaster.common.aspect;

import com.vocabmaster.common.annotation.RateLimit;
import com.vocabmaster.common.constant.RedisKey;
import com.vocabmaster.common.exception.BizException;
import com.vocabmaster.common.result.ErrorCode;
import com.vocabmaster.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(com.vocabmaster.common.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        String identifier = resolveIdentifier(rateLimit.by());
        String apiKey = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        String redisKey = RedisKey.rateLimit(apiKey, identifier);

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == null) {
            return point.proceed();
        }
        if (count == 1) {
            redisTemplate.expire(redisKey, rateLimit.window(), TimeUnit.SECONDS);
        }
        if (count > rateLimit.limit()) {
            log.warn("限流触发 key={} count={}", redisKey, count);
            throw new BizException(ErrorCode.RATE_LIMIT);
        }

        return point.proceed();
    }

    private String resolveIdentifier(String by) {
        if ("ip".equals(by)) {
            HttpServletRequest request = ((ServletRequestAttributes)
                    Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            return (forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr());
        }
        UserContext.Current current = UserContext.get();
        return current != null ? String.valueOf(current.userId()) : "anonymous";
    }
}
