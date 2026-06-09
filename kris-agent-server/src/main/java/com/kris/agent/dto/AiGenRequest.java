package com.kris.agent.dto;

import lombok.Data;

/**
 * AI 生成请求 DTO
 *
 * 【前端类比】相当于前端的 interface AiGenRequest { description: string; role: string }
 * DTO = Data Transfer Object，用于接收前端发来的 JSON 请求体
 * Spring 会自动把请求 JSON 反序列化到这个对象的字段上
 *
 * @Data 是 Lombok 注解，自动生成 getter/setter/toString/equals/hashCode
 * 【前端类比】相当于 TypeScript 的 interface，但 Java 需要 getter/setter 才能序列化
 */
@Data
public class AiGenRequest {
    private String description;
    private String role;
}
