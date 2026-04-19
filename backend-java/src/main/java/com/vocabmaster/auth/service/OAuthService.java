package com.vocabmaster.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocabmaster.common.exception.BizException;
import com.vocabmaster.common.result.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 封装第三方 OAuth 的外部 API 调用（不含用户创建/登录逻辑）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private static final String WECHAT_SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.oauth.wechat.app-id:}")
    private String wechatAppId;

    @Value("${app.oauth.wechat.app-secret:}")
    private String wechatAppSecret;

    // ---- 微信 ----

    public record WechatSession(String openid, String unionid, String sessionKey) {}

    /**
     * 用小程序端的 code 换取微信 openid + session_key。
     * @throws BizException OAUTH_FAILED 如果微信返回错误
     */
    public WechatSession getWechatSession(String code) {
        String url = UriComponentsBuilder.fromHttpUrl(WECHAT_SESSION_URL)
                .queryParam("appid", wechatAppId)
                .queryParam("secret", wechatAppSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(responseBody);

            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                log.error("微信 jscode2session 失败: {}", responseBody);
                throw new BizException(ErrorCode.OAUTH_FAILED, "微信授权失败: " + node.path("errmsg").asText());
            }

            return new WechatSession(
                    node.path("openid").asText(),
                    node.has("unionid") ? node.get("unionid").asText() : null,
                    node.path("session_key").asText()
            );
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信 API 失败", e);
            throw new BizException(ErrorCode.OAUTH_FAILED, "微信授权网络错误");
        }
    }

    // ---- Apple ----

    public record AppleIdentity(String appleUserId, String email) {}

    /**
     * 解码 Apple identity_token，提取 sub（Apple User ID）和 email。
     *
     * TODO: 生产环境需要从 https://appleid.apple.com/auth/keys 获取公钥并验证签名。
     * 当前仅做 JWT 解码（不验签），适用于开发阶段。
     */
    public AppleIdentity parseAppleToken(String identityToken) {
        try {
            // 不验签，仅解码 payload（Base64）
            String[] parts = identityToken.split("\\.");
            if (parts.length < 2) {
                throw new BizException(ErrorCode.OAUTH_FAILED, "Apple token 格式错误");
            }
            byte[] payloadBytes = java.util.Base64.getUrlDecoder()
                    .decode(padBase64(parts[1]));
            JsonNode payload = objectMapper.readTree(payloadBytes);

            String sub = payload.path("sub").asText();
            String email = payload.has("email") ? payload.get("email").asText() : null;

            if (sub.isBlank()) {
                throw new BizException(ErrorCode.OAUTH_FAILED, "Apple token 缺少 sub");
            }
            return new AppleIdentity(sub, email);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 Apple identity_token 失败", e);
            throw new BizException(ErrorCode.OAUTH_FAILED, "Apple 授权失败");
        }
    }

    /** Base64 URL 补齐 padding */
    private String padBase64(String base64) {
        int pad = 4 - base64.length() % 4;
        if (pad < 4) base64 += "=".repeat(pad);
        return base64;
    }
}
