package com.vocabmaster.security;

public final class UserContext {

    public record Current(Long userId, String uuid, String role) {}

    private static final ThreadLocal<Current> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(Current current) {
        HOLDER.set(current);
    }

    public static Current get() {
        return HOLDER.get();
    }

    public static Long currentUserId() {
        Current c = HOLDER.get();
        if (c == null) throw new IllegalStateException("当前请求未登录");
        return c.userId();
    }

    public static String currentUuid() {
        Current c = HOLDER.get();
        if (c == null) throw new IllegalStateException("当前请求未登录");
        return c.uuid();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
