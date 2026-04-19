package com.vocabmaster.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;

/**
 * 手机号加密工具。
 * 加密算法：AES-256-GCM（Bouncy Castle 提供）。
 * 哈希算法：SHA-256（用于建立 phone_hash 唯一索引，不可逆）。
 *
 * 存储格式：Base64(IV[12] || Ciphertext || AuthTag[16])
 */
@Component
@Slf4j
public class PhoneCryptoUtil {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    @Value("${app.aes.key}")
    private String aesKeyStr;

    private SecretKeySpec secretKey;

    @PostConstruct
    void init() throws Exception {
        // SHA-256 派生 32 字节密钥，兼容任意长度的配置字符串
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(aesKeyStr.getBytes(StandardCharsets.UTF_8));
        secretKey = new SecretKeySpec(keyBytes, "AES");
        log.debug("PhoneCryptoUtil 初始化完成");
    }

    public String encrypt(String plainPhone) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plainPhone.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, result, GCM_IV_LENGTH, ciphertext.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("手机号加密失败", e);
        }
    }

    public String decrypt(String encryptedB64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedB64);
            byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(decoded, GCM_IV_LENGTH, decoded.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("手机号解密失败", e);
        }
    }

    /**
     * 手机号 SHA-256 哈希，用于 phone_hash 唯一索引查询。
     * 静态方法，注册前也可调用。
     */
    public static String phoneHash(String phone) {
        try {
            byte[] hashBytes = MessageDigest.getInstance("SHA-256")
                    .digest(phone.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
