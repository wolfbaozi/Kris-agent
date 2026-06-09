package com.kris.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kris.agent.config.EncryptionConfig;
import com.kris.agent.dto.ApiKeyRequest;
import com.kris.agent.entity.ApiKey;
import com.kris.agent.mapper.ApiKeyMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {

    private final ApiKeyMapper apiKeyMapper;
    private final EncryptionConfig encryptionConfig;

    public ApiKeyService(ApiKeyMapper apiKeyMapper, EncryptionConfig encryptionConfig) {
        this.apiKeyMapper = apiKeyMapper;
        this.encryptionConfig = encryptionConfig;
    }

    public List<Map<String, Object>> list(Long userId) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getUserId, userId);
        List<ApiKey> keys = apiKeyMapper.selectList(wrapper);
        return keys.stream().map(k -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", k.getId());
            map.put("provider", k.getProvider());
            map.put("model", k.getModel());
            map.put("baseUrl", k.getBaseUrl());
            map.put("createdAt", k.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    public ApiKey create(Long userId, ApiKeyRequest request) {
        ApiKey apiKey = new ApiKey();
        apiKey.setUserId(userId);
        apiKey.setProvider(request.getProvider());
        apiKey.setEncryptedKey(encryptionConfig.encrypt(request.getApiKey()));
        apiKey.setModel(request.getModel() != null ? request.getModel() : "");
        apiKey.setBaseUrl(request.getBaseUrl() != null ? request.getBaseUrl() : "");
        apiKeyMapper.insert(apiKey);
        return apiKey;
    }

    public void update(Long userId, Long id, ApiKeyRequest request) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getUserId, userId).eq(ApiKey::getId, id);
        ApiKey apiKey = apiKeyMapper.selectOne(wrapper);
        if (apiKey == null) {
            throw new RuntimeException("API Key不存在");
        }
        if (request.getApiKey() != null) {
            apiKey.setEncryptedKey(encryptionConfig.encrypt(request.getApiKey()));
        }
        if (request.getModel() != null) {
            apiKey.setModel(request.getModel());
        }
        if (request.getBaseUrl() != null) {
            apiKey.setBaseUrl(request.getBaseUrl());
        }
        apiKeyMapper.updateById(apiKey);
    }

    public void delete(Long userId, Long id) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getUserId, userId).eq(ApiKey::getId, id);
        if (apiKeyMapper.selectCount(wrapper) == 0) {
            throw new RuntimeException("API Key不存在");
        }
        apiKeyMapper.deleteById(id);
    }
}
