package com.vocabmaster.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * 支持 X-HTTP-Method-Override 头改写请求方法。
 *
 * 背景：wx.request 不支持 PATCH，小程序端统一降级为 POST + 该头透传
 * （wordmate-mini request.ts）。Spring 默认不识别此头，POST 会打到
 * 只有 @PatchMapping 的端点上 405，因此在这里统一改写。
 *
 * 安全约束：只允许把 POST 改写为 PATCH/PUT/DELETE——POST 才有请求体语义，
 * 且不允许改写为 GET/HEAD，避免绕过方法级安全规则。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MethodOverrideFilter extends OncePerRequestFilter {

    private static final String OVERRIDE_HEADER = "X-HTTP-Method-Override";
    private static final Set<String> OVERRIDABLE_METHODS = Set.of("PATCH", "PUT", "DELETE");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String override = request.getHeader(OVERRIDE_HEADER);
        String target = normalize(override);
        if (target != null && "POST".equalsIgnoreCase(request.getMethod())) {
            HttpServletRequest overridden = new HttpServletRequestWrapper(request) {
                @Override
                public String getMethod() {
                    return target;
                }
            };
            chain.doFilter(overridden, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private String normalize(String method) {
        if (method == null) {
            return null;
        }
        String upper = method.trim().toUpperCase(Locale.ROOT);
        return OVERRIDABLE_METHODS.contains(upper) ? upper : null;
    }
}
