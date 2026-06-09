package com.kris.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 加解密工具
 *
 * 【前端类比】相当于前端的 crypto-js 工具库，用于对敏感数据（如 API Key）做加密存储
 * @Component 表示这是一个全局单例的 Bean，任何地方都可以通过构造器注入来使用
 *
 * 为什么用 AES-GCM 而不是 AES-CBC？
 * GCM 模式自带完整性校验（类似 JWT 的签名），能防止密文被篡改
 */
@Component
public class EncryptionConfig {

    private final SecretKeySpec keySpec;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * 构造器注入密钥
     * @Value("${app.encryption.key}") 从 application.yml 中读取配置项
     * 【前端类比】相当于 import.meta.env.VITE_ENCRYPTION_KEY
     */
    public EncryptionConfig(@Value("${app.encryption.key}") String key) {
        this.keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
    }

    /**
     * 加密：明文 -> Base64(IV + 密文)
     * 每次加密都会生成随机 IV（初始化向量），相同明文加密结果不同，更安全
     */
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            // 把 IV 和密文拼在一起存储，解密时再拆开
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密：Base64(IV + 密文) -> 明文
     * 先 Base64 解码，再拆出 IV 和密文，最后用相同密钥解密
     */
    public String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
}
