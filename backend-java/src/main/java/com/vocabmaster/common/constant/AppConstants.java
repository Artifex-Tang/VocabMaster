package com.vocabmaster.common.constant;

public final class AppConstants {

    private AppConstants() {}

    // 用户角色
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    // 答题结果
    public static final String ANSWER_CORRECT = "correct";
    public static final String ANSWER_WRONG = "wrong";
    public static final String ANSWER_SKIP = "skip";

    // 学习动作
    public static final String ACTION_LEARN = "learn";
    public static final String ACTION_REVIEW = "review";
    public static final String ACTION_TEST = "test";

    // 学习模式
    public static final String MODE_CARD = "card";
    public static final String MODE_SPELLING = "spelling";
    public static final String MODE_CHOICE = "choice";
    public static final String MODE_LISTENING = "listening";

    // 艾宾浩斯阶段边界
    public static final int MIN_STAGE = 0;
    public static final int MAX_STAGE = 9;

    // 分页默认值
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 200;

    // 词库审核状态
    public static final int AUDIT_PASS = 1;
    public static final int AUDIT_PENDING = 0;
    public static final int AUDIT_REJECT = -1;

    // 用户状态
    public static final int USER_STATUS_ACTIVE = 1;
    public static final int USER_STATUS_DISABLED = 0;

    // 错题本：连续答对 N 次自动 resolved
    public static final int WRONG_WORD_RESOLVE_THRESHOLD = 3;

    // 验证码有效期（秒）
    public static final int VERIFY_CODE_TTL_SECONDS = 300;

    // 客户端时间戳最大允许超前服务端的毫秒数
    public static final long CLIENT_TS_MAX_FUTURE_MS = 60_000L;
}
