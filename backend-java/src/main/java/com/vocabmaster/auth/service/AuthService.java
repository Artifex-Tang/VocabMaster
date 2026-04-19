package com.vocabmaster.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vocabmaster.auth.dto.*;
import com.vocabmaster.checkin.entity.UserStreak;
import com.vocabmaster.checkin.mapper.UserStreakMapper;
import com.vocabmaster.common.constant.AppConstants;
import com.vocabmaster.common.constant.RedisKey;
import com.vocabmaster.common.exception.BizException;
import com.vocabmaster.common.result.ErrorCode;
import com.vocabmaster.security.JwtUtil;
import com.vocabmaster.user.entity.User;
import com.vocabmaster.user.entity.UserAuth;
import com.vocabmaster.user.entity.UserSettings;
import com.vocabmaster.user.mapper.UserAuthMapper;
import com.vocabmaster.user.mapper.UserMapper;
import com.vocabmaster.user.mapper.UserSettingsMapper;
import com.vocabmaster.util.PhoneCryptoUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final UserStreakMapper userStreakMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeService codeService;
    private final OAuthService oAuthService;
    private final StringRedisTemplate redisTemplate;
    private final PhoneCryptoUtil phoneCryptoUtil;

    // ===== 注册 =====

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        validatePasswordStrength(req.getPassword());

        if ("email".equals(req.getType())) {
            validateEmail(req.getIdentifier());
            if (userMapper.findByEmail(req.getIdentifier()) != null) {
                throw new BizException(ErrorCode.ACCOUNT_EXISTS);
            }
        } else {
            validatePhone(req.getIdentifier());
            if (req.getCode() == null || req.getCode().isBlank()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "手机号注册需要验证码");
            }
            codeService.verifyAndConsume("phone", req.getIdentifier(), "register", req.getCode());
            String phoneHash = PhoneCryptoUtil.phoneHash(req.getIdentifier());
            if (userMapper.findByPhoneHash(phoneHash) != null) {
                throw new BizException(ErrorCode.ACCOUNT_EXISTS);
            }
        }

        User user = buildNewUser(req);
        userMapper.insert(user);
        createDefaultSettings(user.getId());

        return issueTokens(user);
    }

    // ===== 密码登录 =====

    public AuthResponse login(LoginRequest req) {
        User user = findUserByIdentifier(req.getType(), req.getIdentifier());
        checkUserActive(user);

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.PASSWORD_WRONG);
        }

        touchLastLogin(user.getId());
        return issueTokens(user);
    }

    // ===== 验证码登录（手机号，不存在则自动注册）=====

    @Transactional
    public AuthResponse loginByCode(LoginByCodeRequest req) {
        validatePhone(req.getIdentifier());
        codeService.verifyAndConsume("phone", req.getIdentifier(), "login", req.getCode());

        String phoneHash = PhoneCryptoUtil.phoneHash(req.getIdentifier());
        User user = userMapper.findByPhoneHash(phoneHash);

        if (user == null) {
            // 自动注册
            user = User.builder()
                    .uuid(UUID.randomUUID().toString())
                    .phone(phoneCryptoUtil.encrypt(req.getIdentifier()))
                    .phoneHash(phoneHash)
                    .role(AppConstants.ROLE_USER)
                    .status(AppConstants.USER_STATUS_ACTIVE)
                    .build();
            userMapper.insert(user);
            createDefaultSettings(user.getId());
        } else {
            checkUserActive(user);
            touchLastLogin(user.getId());
        }

        return issueTokens(user);
    }

    // ===== 微信小程序登录 =====

    @Transactional
    public AuthResponse loginWechat(LoginWechatRequest req) {
        OAuthService.WechatSession session = oAuthService.getWechatSession(req.getCode());

        UserAuth userAuth = userAuthMapper.findByProviderAndId("wechat", session.openid());

        User user;
        if (userAuth != null) {
            user = userMapper.selectById(userAuth.getUserId());
            if (user == null) throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
            checkUserActive(user);
            touchLastLogin(user.getId());
        } else {
            String nickname = null;
            String avatarUrl = null;
            if (req.getUserInfo() != null) {
                nickname = req.getUserInfo().getNickname();
                avatarUrl = req.getUserInfo().getAvatarUrl();
            }
            user = User.builder()
                    .uuid(UUID.randomUUID().toString())
                    .nickname(nickname)
                    .avatarUrl(avatarUrl)
                    .role(AppConstants.ROLE_USER)
                    .status(AppConstants.USER_STATUS_ACTIVE)
                    .build();
            userMapper.insert(user);
            createDefaultSettings(user.getId());

            userAuthMapper.insert(UserAuth.builder()
                    .userId(user.getId())
                    .provider("wechat")
                    .providerUserId(session.openid())
                    .unionId(session.unionid())
                    .build());
        }

        return issueTokens(user);
    }

    // ===== Apple 登录 =====

    @Transactional
    public AuthResponse loginApple(LoginAppleRequest req) {
        OAuthService.AppleIdentity identity = oAuthService.parseAppleToken(req.getIdentityToken());

        UserAuth userAuth = userAuthMapper.findByProviderAndId("apple", identity.appleUserId());

        User user;
        if (userAuth != null) {
            user = userMapper.selectById(userAuth.getUserId());
            if (user == null) throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
            checkUserActive(user);
            touchLastLogin(user.getId());
        } else {
            user = User.builder()
                    .uuid(UUID.randomUUID().toString())
                    .email(identity.email())
                    .role(AppConstants.ROLE_USER)
                    .status(AppConstants.USER_STATUS_ACTIVE)
                    .build();
            userMapper.insert(user);
            createDefaultSettings(user.getId());

            userAuthMapper.insert(UserAuth.builder()
                    .userId(user.getId())
                    .provider("apple")
                    .providerUserId(identity.appleUserId())
                    .build());
        }

        return issueTokens(user);
    }

    // ===== 刷新 Token =====

    public AuthResponse refresh(RefreshTokenRequest req) {
        Claims claims;
        try {
            claims = jwtUtil.parse(req.getRefreshToken());
        } catch (BizException e) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String uuid = claims.getSubject();

        // 验证 Redis 中存储的 refresh token 是否一致（防止重放）
        String storedToken = redisTemplate.opsForValue().get(RedisKey.REFRESH_TOKEN + uuid);
        if (!req.getRefreshToken().equals(storedToken)) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        User user = userMapper.findByUuid(uuid);
        if (user == null) throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        checkUserActive(user);

        return issueTokens(user);
    }

    // ===== 登出 =====

    public void logout(String accessToken) {
        try {
            Claims claims = jwtUtil.parse(accessToken);
            long ttl = jwtUtil.remainingTtlSeconds(claims);
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        RedisKey.TOKEN_BLACKLIST + accessToken, "1", ttl, TimeUnit.SECONDS);
            }
            // 同步吊销 refresh token
            redisTemplate.delete(RedisKey.REFRESH_TOKEN + claims.getSubject());
        } catch (BizException e) {
            // token 已过期，登出无需处理
            log.debug("登出时 accessToken 已过期");
        }
    }

    // ===== 重置密码 =====

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        validatePasswordStrength(req.getNewPassword());
        codeService.verifyAndConsume(req.getType(), req.getIdentifier(), "reset_password", req.getCode());

        User user = findUserByIdentifier(req.getType(), req.getIdentifier());
        checkUserActive(user);

        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getPasswordHash, passwordEncoder.encode(req.getNewPassword())));
    }

    // ===== 发送验证码 =====

    public int sendCode(SendCodeRequest req) {
        return codeService.sendCode(req.getType(), req.getIdentifier(), req.getScene());
    }

    // ===== 包级可见：供 OAuthService 等同包类使用 =====

    public AuthResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getUuid(), user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUuid());

        redisTemplate.opsForValue().set(
                RedisKey.REFRESH_TOKEN + user.getUuid(),
                refreshToken,
                jwtUtil.getRefreshTtlSeconds(),
                TimeUnit.SECONDS);

        return AuthResponse.builder()
                .user(UserDto.from(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTtlSeconds())
                .build();
    }

    // ===== private helpers =====

    private User buildNewUser(RegisterRequest req) {
        User.UserBuilder builder = User.builder()
                .uuid(UUID.randomUUID().toString())
                .nickname(req.getNickname())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(AppConstants.ROLE_USER)
                .status(AppConstants.USER_STATUS_ACTIVE);

        if ("email".equals(req.getType())) {
            builder.email(req.getIdentifier());
        } else {
            builder.phone(phoneCryptoUtil.encrypt(req.getIdentifier()))
                   .phoneHash(PhoneCryptoUtil.phoneHash(req.getIdentifier()));
        }
        return builder.build();
    }

    private void createDefaultSettings(Long userId) {
        userSettingsMapper.insert(UserSettings.builder().userId(userId).build());
        userStreakMapper.insert(UserStreak.builder().userId(userId).build());
    }

    private User findUserByIdentifier(String type, String identifier) {
        User user;
        if ("email".equals(type)) {
            user = userMapper.findByEmail(identifier);
        } else {
            String phoneHash = PhoneCryptoUtil.phoneHash(identifier);
            user = userMapper.findByPhoneHash(phoneHash);
        }
        if (user == null) throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        return user;
    }

    private void checkUserActive(User user) {
        if (user.getStatus() == null || user.getStatus() != AppConstants.USER_STATUS_ACTIVE) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    private void touchLastLogin(Long userId) {
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getLastLoginAt, LocalDateTime.now(ZoneOffset.UTC)));
    }

    private void validatePasswordStrength(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "密码至少 8 位且同时包含字母和数字");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "邮箱格式不正确");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "手机号格式不正确（仅支持中国大陆号码）");
        }
    }
}
