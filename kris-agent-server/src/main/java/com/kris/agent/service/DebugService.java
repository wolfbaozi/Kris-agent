package com.kris.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kris.agent.dto.DebugRequest;
import com.kris.agent.entity.Skill;
import com.kris.agent.mapper.SkillMapper;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调试服务 —— Skill/MCP 在线调试
 *
 * 【前端类比】相当于前端的"调试面板"功能
 * 与 ChatService 的 SSE 实现方式不同：
 *   - ChatService 用 SseEmitter（Spring 封装好的 SSE 工具）
 *   - DebugService 直接操作 HttpServletResponse 的 PrintWriter（更底层）
 *
 * 两种方式效果一样，都是往响应里写 "data: {...}\n\n" 格式的 SSE 数据
 * 这里选择直接操作 response 是因为调试场景更简单，不需要 SseEmitter 的超时管理等特性
 */
@Service
public class DebugService {

    private final ObjectMapper objectMapper;
    private final SkillMapper skillMapper;

    public DebugService(ObjectMapper objectMapper, SkillMapper skillMapper) {
        this.objectMapper = objectMapper;
        this.skillMapper = skillMapper;
    }

    /**
     * 调试 Skill：
     * 1. 设置 SSE 响应头
     * 2. 解析前端传来的表单数据
     * 3. 创建临时 Skill 记录
     * 4. 通过 SSE 推送调试结果
     */
    public void debugSkill(Long userId, DebugRequest request, HttpServletResponse response) throws IOException {
        // 设置 SSE 响应头（告诉浏览器这是一个事件流）
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        PrintWriter writer = response.getWriter();

        try {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                writeSSE(writer, "error", singleMap("error", "Skill名称不能为空"));
                writer.close();
                return;
            }

            // 解析 JSON 文本字段（前端传的是字符串，需要反序列化）
            // TypeReference 相当于 TypeScript 的泛型类型参数：JSON.parse<Map<string, object>>(text)
            Map<String, Object> properties = Collections.emptyMap();
            List<String> required = Collections.emptyList();
            if (request.getPropertiesText() != null && !request.getPropertiesText().trim().isEmpty()) {
                properties = objectMapper.readValue(request.getPropertiesText(),
                        new TypeReference<Map<String, Object>>() {});
            }
            if (request.getRequiredText() != null && !request.getRequiredText().trim().isEmpty()) {
                required = objectMapper.readValue(request.getRequiredText(),
                        new TypeReference<List<String>>() {});
            }

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "object");
            parameters.put("properties", properties);
            parameters.put("required", required);

            Map<String, Object> schema = new HashMap<>();
            schema.put("description", request.getName());
            schema.put("parameters", parameters);

            Skill skill = new Skill();
            skill.setUserId(userId);
            skill.setName(request.getName());
            skill.setSkillType(request.getSkillType() != null ? request.getSkillType() : "tool");
            skill.setSourceType("database");
            skill.setToolSchema(objectMapper.writeValueAsString(schema));
            skill.setToolCode(request.getToolCode());
            skill.setPromptContent(request.getPromptContent());
            skillMapper.insert(skill);

            writeSSE(writer, "text-delta", singleMap("content", "已创建 Skill，开始调试...\n\n"));
            writeSSE(writer, "done", new HashMap<>());

        } catch (Exception e) {
            writeSSE(writer, "error", singleMap("error", e.getMessage()));
        }
        writer.close();
    }

    public void debugMcp(Long userId, DebugRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        PrintWriter writer = response.getWriter();

        try {
            String msg = "MCP 调试模式已启动，发送测试消息: " +
                    (request.getTestMessage() != null ? request.getTestMessage() : "");
            writeSSE(writer, "text-delta", singleMap("content", msg));
            writeSSE(writer, "done", new HashMap<>());
        } catch (Exception e) {
            writeSSE(writer, "error", singleMap("error", e.getMessage()));
        }
        writer.close();
    }

    /**
     * 写入一条 SSE 事件
     * 格式：data: {"type":"xxx",...}\n\n
     * 【前端类比】相当于前端 EventSource 收到的每一行 "data: ..." 消息
     */
    private void writeSSE(PrintWriter writer, String type, Map<String, Object> data) {
        try {
            Map<String, Object> msg = new HashMap<>(data);
            msg.put("type", type);
            writer.write("data: " + objectMapper.writeValueAsString(msg) + "\n\n");
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    private Map<String, Object> singleMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
