package com.vocabmaster.common.annotation;

import java.lang.annotation.*;

/**
 * 标记需要管理员权限的接口（SecurityConfig 已通过 hasRole("ADMIN") 统一拦截 /admin/**）。
 * 此注解供文档和代码可读性使用。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {
}
