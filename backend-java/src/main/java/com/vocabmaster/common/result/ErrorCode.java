package com.vocabmaster.common.result;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "ok"),
    // 10xxx — 通用
    PARAM_INVALID(10001, "参数校验失败"),
    RATE_LIMIT(10002, "请求过于频繁"),
    CONTENT_TYPE_UNSUPPORTED(10003, "不支持的 Content-Type"),
    CLIENT_ERROR(10099, "客户端错误"),
    // 20xxx — 认证授权
    UNAUTHORIZED(20001, "未登录或 token 无效"),
    TOKEN_EXPIRED(20002, "token 已过期"),
    FORBIDDEN(20003, "无权限"),
    REFRESH_TOKEN_INVALID(20004, "refresh_token 无效"),
    // 30xxx — 用户
    ACCOUNT_EXISTS(30001, "账号已存在"),
    VERIFY_CODE_INVALID(30002, "验证码错误或已过期"),
    ACCOUNT_NOT_FOUND(30003, "账号不存在"),
    PASSWORD_WRONG(30004, "密码错误"),
    ACCOUNT_DISABLED(30005, "账号已禁用"),
    OAUTH_FAILED(30006, "第三方登录失败"),
    // 40xxx — 词库/学习
    WORD_NOT_FOUND(40001, "词条不存在"),
    LEVEL_NOT_FOUND(40002, "等级不存在"),
    WORD_NOT_BELONG(40003, "不属于该用户"),
    ANSWER_INVALID(40004, "无效的答题结果"),
    TEST_INVALID(40005, "测试 ID 无效或已过期"),
    // 50xxx — 同步
    SYNC_CONFLICT(50001, "同步冲突"),
    CLIENT_TS_TOO_OLD(50002, "client_ts 过旧"),
    // 90xxx — 管理后台
    ADMIN_ONLY(90001, "仅管理员可访问"),
    WORD_DUPLICATE(90002, "词条重复"),
    // 99xxx — 服务端
    SERVER_ERROR(99999, "服务端错误");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
