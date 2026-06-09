package com.kris.agent.dto;

import lombok.Data;

/**
 * API Key 请求 DTO
 *
 * 【前端类比】相当于前端的 interface ApiKeyRequest { provider: string; apiKey: string; ... }
 * 用于接收创建/更新 API Key 时的请求体
 */
@Data
public class ApiKeyRequest {
    private String provider;
    private String apiKey;
    private String model;
    private String baseUrl;
}
