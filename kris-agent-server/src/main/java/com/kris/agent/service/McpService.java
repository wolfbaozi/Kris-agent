package com.kris.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kris.agent.dto.McpRequest;
import com.kris.agent.entity.McpServer;
import com.kris.agent.mapper.McpServerMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP 服务器管理服务 —— CRUD + 启用/禁用
 *
 * 【前端类比】相当于前端的 useMcpServers() composable
 *
 * ObjectMapper 是 Java 的 JSON 工具（相当于 JSON.stringify / JSON.parse）
 * args/env/config 在数据库里存 JSON 字符串，读写时需要序列化/反序列化
 *
 * 数据权限隔离：
 *   - 每个用户只能操作自己的 MCP（where userId = ?）
 *   - isGlobal = 1 的是全局 MCP，所有人可见
 *   - 类似前端的"个人配置 vs 全局配置"
 */
@Service
public class McpService {

    private final McpServerMapper mcpServerMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.run-env:production}")
    private String currentEnv;

    public McpService(McpServerMapper mcpServerMapper, ObjectMapper objectMapper) {
        this.mcpServerMapper = mcpServerMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 列表查询：只返回已启用的 + 匹配当前运行环境的 + 自己的或全局的
     * 【前端类比】相当于前端列表页的过滤条件：enabled=true && (runEnv='all' || runEnv=current)
     */
    public List<Map<String, Object>> list(Long userId) {
        LambdaQueryWrapper<McpServer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpServer::getEnabled, 1)
                .and(w -> w.eq(McpServer::getRunEnv, "all").or().eq(McpServer::getRunEnv, currentEnv))
                .and(w -> w.eq(McpServer::getUserId, userId).or().eq(McpServer::getIsGlobal, 1));
        List<McpServer> servers = mcpServerMapper.selectList(wrapper);
        return servers.stream().map(s -> toMap(s, userId)).collect(Collectors.toList());
    }

    public Map<String, Object> create(Long userId, McpRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("MCP名称不能为空");
        }
        // 重名校验（同一用户下名称唯一）
        LambdaQueryWrapper<McpServer> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(McpServer::getUserId, userId).eq(McpServer::getName, request.getName());
        if (mcpServerMapper.selectCount(dupWrapper) > 0) {
            throw new RuntimeException("MCP名称\"" + request.getName() + "\"已存在");
        }
        McpServer server = new McpServer();
        server.setUserId(userId);
        server.setName(request.getName());
        server.setRunEnv(request.getRunEnv() != null ? request.getRunEnv() : "all");
        server.setSourceType(request.getSourceType() != null ? request.getSourceType() : "database");
        server.setServerType(request.getServerType() != null ? request.getServerType() : "");
        server.setCommand(request.getCommand() != null ? request.getCommand() : "");
        // JSON 对象字段需要序列化为字符串存储（相当于 JSON.stringify）
        if (request.getArgs() != null) {
            try {
                server.setArgs(objectMapper.writeValueAsString(request.getArgs()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("args序列化失败");
            }
        }
        if (request.getEnv() != null) {
            try {
                server.setEnv(objectMapper.writeValueAsString(request.getEnv()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("env序列化失败");
            }
        }
        server.setFilePath(request.getFilePath() != null ? request.getFilePath() : "");
        if (request.getConfig() != null) {
            try {
                server.setConfig(objectMapper.writeValueAsString(request.getConfig()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("config序列化失败");
            }
        }
        mcpServerMapper.insert(server);
        Map<String, Object> result = new HashMap<>();
        result.put("id", server.getId());
        result.put("name", server.getName());
        return result;
    }

    public void update(Long userId, Long id, McpRequest request) {
        LambdaQueryWrapper<McpServer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpServer::getUserId, userId).eq(McpServer::getId, id);
        McpServer server = mcpServerMapper.selectOne(wrapper);
        if (server == null) {
            throw new RuntimeException("MCP配置不存在或无权限修改");
        }
        // 改名时检查新名称是否与其他记录冲突（排除自己）
        if (request.getName() != null && !request.getName().equals(server.getName())) {
            LambdaQueryWrapper<McpServer> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.eq(McpServer::getUserId, userId)
                    .eq(McpServer::getName, request.getName())
                    .ne(McpServer::getId, id);
            if (mcpServerMapper.selectCount(dupWrapper) > 0) {
                throw new RuntimeException("MCP名称\"" + request.getName() + "\"已存在");
            }
            server.setName(request.getName());
        }
        if (request.getRunEnv() != null) server.setRunEnv(request.getRunEnv());
        if (request.getSourceType() != null) server.setSourceType(request.getSourceType());
        if (request.getServerType() != null) server.setServerType(request.getServerType());
        if (request.getCommand() != null) server.setCommand(request.getCommand());
        if (request.getArgs() != null) {
            try { server.setArgs(objectMapper.writeValueAsString(request.getArgs())); }
            catch (JsonProcessingException e) { throw new RuntimeException("args序列化失败"); }
        }
        if (request.getEnv() != null) {
            try { server.setEnv(objectMapper.writeValueAsString(request.getEnv())); }
            catch (JsonProcessingException e) { throw new RuntimeException("env序列化失败"); }
        }
        if (request.getFilePath() != null) server.setFilePath(request.getFilePath());
        if (request.getConfig() != null) {
            try { server.setConfig(objectMapper.writeValueAsString(request.getConfig())); }
            catch (JsonProcessingException e) { throw new RuntimeException("config序列化失败"); }
        }
        if (request.getIsGlobal() != null) server.setIsGlobal(request.getIsGlobal());
        mcpServerMapper.updateById(server);
    }

    public void delete(Long userId, Long id) {
        LambdaQueryWrapper<McpServer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpServer::getUserId, userId).eq(McpServer::getId, id);
        if (mcpServerMapper.selectCount(wrapper) == 0) {
            throw new RuntimeException("MCP配置不存在或无权限删除");
        }
        mcpServerMapper.deleteById(id);
    }

    /**
     * 切换启用/禁用：enabled 在 0 和 1 之间翻转
     */
    public Map<String, Object> toggle(Long userId, Long id) {
        LambdaQueryWrapper<McpServer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpServer::getUserId, userId).eq(McpServer::getId, id);
        McpServer server = mcpServerMapper.selectOne(wrapper);
        if (server == null) {
            throw new RuntimeException("MCP配置不存在或无权限操作");
        }
        int newEnabled = (server.getEnabled() != null && server.getEnabled() == 1) ? 0 : 1;
        server.setEnabled(newEnabled);
        mcpServerMapper.updateById(server);
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", newEnabled == 1);
        return result;
    }

    /**
     * Entity -> Map 转换（控制返回给前端的字段）
     * 全局 MCP 不暴露 command/args/env（安全考虑）
     */
    private Map<String, Object> toMap(McpServer server, Long currentUserId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", server.getId());
        map.put("userId", server.getUserId());
        map.put("name", server.getName());
        map.put("isGlobal", server.getIsGlobal());
        map.put("runEnv", server.getRunEnv());
        map.put("sourceType", server.getSourceType());
        map.put("serverType", server.getServerType());
        map.put("filePath", server.getFilePath());
        map.put("enabled", server.getEnabled());
        map.put("config", server.getConfig());
        map.put("createdAt", server.getCreatedAt());
        if (server.getIsGlobal() != 1 && server.getUserId().equals(currentUserId)) {
            map.put("command", server.getCommand());
            map.put("args", server.getArgs());
            map.put("env", server.getEnv());
        }
        return map;
    }
}
