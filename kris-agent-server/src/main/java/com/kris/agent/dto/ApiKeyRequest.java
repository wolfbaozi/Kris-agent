package com.kris.agent.dto;

import lombok.Data;

@Data
public class ApiKeyRequest {
    private String provider;
    private String apiKey;
    private String model;
    private String baseUrl;
}
