package com.vocabmaster.auth.service;

import com.vocabmaster.common.constant.AppConstants;
import com.vocabmaster.common.constant.RedisKey;
import com.vocabmaster.common.exception.BizException;
import com.vocabmaster.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeService {

    private static final int CODE_LENGTH = 6;
    private static final int DAY_LIMIT = 10;
    private static final int MINUTE_LIMIT_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    /**
     * 发送验证码（dev 环境直接 log，prod 接入短信/邮件服务商）。
     * 限流：同一 identifier 每分钟 1 次，每天 10 次。
     */
    public int sendCode(String type, String identifier, String scene) {
        checkSendRateLimit(type, identifier);

        String code = generateCode();
        String key = RedisKey.verifyCode(scene, type, identifier);
        redisTemplate.opsForValue().set(key, code, AppConstants.VERIFY_CODE_TTL_SECONDS, TimeUnit.SECONDS);

        incrementDayCount(type, identifier);

        // TODO: prod 环境接入短信（阿里云 / 腾讯云）或邮件服务
        log.info("[DEV] 验证码 type={} identifier={} scene={} code={}", type, identifier, scene, code);

        return AppConstants.VERIFY_CODE_TTL_SECONDS;
    }

    /**
     * 校验验证码并消费（验证通过后立即删除，防止重放）。
     * @throws BizException VERIFY_CODE_INVALID 如果不匹配或已过期
     */
    public void verifyAndConsume(String type, String identifier, String scene, String inputCode) {
        String key = RedisKey.verifyCode(scene, type, identifier);
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null || !storedCode.equals(inputCode)) {
            throw new BizException(ErrorCode.VERIFY_CODE_INVALID);
        }
        redisTemplate.delete(key);
    }

    // ---- private ----

    private void checkSendRateLimit(String type, String identifier) {
        // 每分钟 1 次（用 TTL 检测）
        String minuteKey = RedisKey.verifyCode("_minute_lock", type, identifier);
        Boolean firstSend = redisTemplate.opsForValue()
                .setIfAbsent(minuteKey, "1", MINUTE_LIMIT_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(firstSend)) {
            throw new BizException(ErrorCode.RATE_LIMIT, "发送过于频繁，请 60 秒后重试");
        }

        // 每天 10 次
        String dayKey = RedisKey.verifyCodeDayCount(type, identifier);
        String countStr = redisTemplate.opsForValue().get(dayKey);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        if (count >= DAY_LIMIT) {
            throw new BizException(ErrorCode.RATE_LIMIT, "今日发送次数已达上限");
        }
    }

    private void incrementDayCount(String type, String identifier) {
        String dayKey = RedisKey.verifyCodeDayCount(type, identifier);
        redisTemplate.opsForValue().increment(dayKey);
        // TTL 设置到当天结束
        long secondsUntilMidnight = Duration.between(
                LocalTime.now(ZoneOffset.UTC),
                LocalTime.MIDNIGHT.minus(1, ChronoUnit.NANOS)
        ).getSeconds() + 86400; // 确保过了午夜
        redisTemplate.expire(dayKey, secondsUntilMidnight % 86400 == 0 ? 86400 : secondsUntilMidnight % 86400, TimeUnit.SECONDS);
    }

    private String generateCode() {
        int n = ThreadLocalRandom.current().nextInt(0, (int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", n);
    }
}
