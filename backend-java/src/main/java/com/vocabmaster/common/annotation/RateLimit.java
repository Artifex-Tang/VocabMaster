package com.vocabmaster.common.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解。由 RateLimitAspect 基于 Redis 计数器实现。
 * limit / window 与 docs/03-api-specification.md 限流配置对齐。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 时间窗口内允许的最大请求次数 */
    int limit() default 60;

    /** 时间窗口，单位：秒 */
    int window() default 60;

    /** 限流维度：user（按用户）或 ip（按 IP） */
    String by() default "user";
}
