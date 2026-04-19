package com.vocabmaster.common.constant;

public final class RedisKey {

    private RedisKey() {}

    // 格式说明：前缀 + 业务维度，key 之间用 : 分隔
    // 例：token:blacklist:eyJ...

    /** JWT 黑名单（登出的 accessToken），TTL = token 剩余有效期 */
    public static final String TOKEN_BLACKLIST = "token:blacklist:";

    /** refreshToken → userId 映射，TTL = refreshToken 有效期 */
    public static final String REFRESH_TOKEN = "token:refresh:";

    /** 验证码，TTL 5 分钟。key = code:scene:type:identifier */
    public static final String VERIFY_CODE = "code:%s:%s:%s";

    /** 验证码发送计数（每日），TTL = 次日 0 点 */
    public static final String VERIFY_CODE_DAY_COUNT = "code:day_count:%s:%s";

    /** 今日学习计划缓存，TTL 10 分钟。key = study:plan:userId:levelCode:date */
    public static final String STUDY_PLAN = "study:plan:%d:%s:%s";

    /** 用户信息缓存，TTL 30 分钟 */
    public static final String USER_INFO = "user:info:%d";

    /** 用户设置缓存，TTL 30 分钟 */
    public static final String USER_SETTINGS = "user:settings:%d";

    /** 等级列表缓存，永久（手动失效）*/
    public static final String LEVEL_LIST = "word:levels";

    /** 主题列表缓存，永久（手动失效）*/
    public static final String TOPIC_LIST = "word:topics";

    /** 连续打卡天数，永久（定时任务维护）*/
    public static final String USER_STREAK = "checkin:streak:%d";

    /** 测试会话，TTL 1 小时。key = test:session:{testId} */
    public static final String TEST_SESSION = "test:session:%s";

    /** 接口限流计数，TTL = window 秒 */
    public static final String RATE_LIMIT = "rate:%s:%s";

    // ---- 便捷格式化方法 ----

    public static String studyPlan(long userId, String levelCode, String date) {
        return String.format(STUDY_PLAN, userId, levelCode, date);
    }

    public static String verifyCode(String scene, String type, String identifier) {
        return String.format(VERIFY_CODE, scene, type, identifier);
    }

    public static String verifyCodeDayCount(String type, String identifier) {
        return String.format(VERIFY_CODE_DAY_COUNT, type, identifier);
    }

    public static String userInfo(long userId) {
        return String.format(USER_INFO, userId);
    }

    public static String userSettings(long userId) {
        return String.format(USER_SETTINGS, userId);
    }

    public static String userStreak(long userId) {
        return String.format(USER_STREAK, userId);
    }

    public static String rateLimit(String apiKey, String identifier) {
        return String.format(RATE_LIMIT, apiKey, identifier);
    }

    public static String testSession(String testId) {
        return String.format(TEST_SESSION, testId);
    }
}
