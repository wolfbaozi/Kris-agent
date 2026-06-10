package com.kris.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kris.agent.entity.McpServer;
import com.kris.agent.entity.User;
import com.kris.agent.mapper.McpServerMapper;
import com.kris.agent.mapper.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class McpRuntimeService {

    private final McpServerMapper mcpServerMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    private final Map<Long, Process> runningProcesses = new ConcurrentHashMap<>();
    private final Map<Long, BufferedReader> processReaders = new ConcurrentHashMap<>();
    private final Map<Long, OutputStream> processWriters = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> requestIds = new ConcurrentHashMap<>();
    private final Map<Long, Long> processUserId = new ConcurrentHashMap<>();

    public McpRuntimeService(McpServerMapper mcpServerMapper, UserMapper userMapper, ObjectMapper objectMapper) {
        this.mcpServerMapper = mcpServerMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> checkQuota(Long userId) {
        User user = userMapper.selectById(userId);
        int maxMcp = user.getMaxMcpCount() != null ? user.getMaxMcpCount() : 10;
        int maxConcurrent = user.getMaxConcurrentMcp() != null ? user.getMaxConcurrentMcp() : 5;

        LambdaQueryWrapper<McpServer> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(McpServer::getUserId, userId);
        long totalMcp = mcpServerMapper.selectCount(countWrapper);

        LambdaQueryWrapper<McpServer> enabledWrapper = new LambdaQueryWrapper<>();
        enabledWrapper.eq(McpServer::getUserId, userId).eq(McpServer::getEnabled, 1);
        long enabledMcp = mcpServerMapper.selectCount(enabledWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("maxMcpCount", maxMcp);
        result.put("maxConcurrentMcp", maxConcurrent);
        result.put("totalMcp", totalMcp);
        result.put("enabledMcp", enabledMcp);
        result.put("canCreate", totalMcp < maxMcp);
        result.put("canEnable", enabledMcp < maxConcurrent);
        return result;
    }

    public List<Long> getRunningStatus(Long userId) {
        List<Long> runningIds = new ArrayList<>();
        Iterator<Map.Entry<Long, Process>> iterator = runningProcesses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Process> entry = iterator.next();
            Long mcpId = entry.getKey();
            Process process = entry.getValue();
            if (!process.isAlive()) {
                iterator.remove();
                processReaders.remove(mcpId);
                processWriters.remove(mcpId);
                requestIds.remove(mcpId);
                processUserId.remove(mcpId);
            } else if (userId.equals(processUserId.get(mcpId))) {
                runningIds.add(mcpId);
            }
        }
        return runningIds;
    }

    public void startMcp(Long userId, Long mcpId) {
        if (isRunning(mcpId)) {
            throw new RuntimeException("MCP已在运行中");
        }

        checkQuotaAndThrow(userId);

        LambdaQueryWrapper<McpServer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpServer::getId, mcpId).eq(McpServer::getUserId, userId);
        McpServer server = mcpServerMapper.selectOne(wrapper);
        if (server == null) throw new RuntimeException("MCP配置不存在");
        if (server.getEnabled() != 1) throw new RuntimeException("MCP未启用");

        stopMcp(userId, mcpId);

        try {
            List<String> command = buildCommand(server);
            ProcessBuilder pb = new ProcessBuilder(command);

            Map<String, String> envMap = pb.environment();
            if (server.getEnv() != null && !server.getEnv().isEmpty()) {
                Map envVars = objectMapper.readValue(server.getEnv(), Map.class);
                envVars.forEach((k, v) -> envMap.put(String.valueOf(k), String.valueOf(v)));
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            runningProcesses.put(mcpId, process);
            processReaders.put(mcpId, new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)));
            processWriters.put(mcpId, process.getOutputStream());
            requestIds.put(mcpId, new AtomicInteger(1));
            processUserId.put(mcpId, userId);

        } catch (Exception e) {
            throw new RuntimeException("启动MCP服务失败: " + e.getMessage());
        }
    }

    public void stopMcp(Long userId, Long mcpId) {
        Process process = runningProcesses.remove(mcpId);
        if (process != null) {
            process.destroy();
        }
        BufferedReader reader = processReaders.remove(mcpId);
        if (reader != null) {
            try { reader.close(); } catch (Exception ignored) {}
        }
        OutputStream writer = processWriters.remove(mcpId);
        if (writer != null) {
            try { writer.close(); } catch (Exception ignored) {}
        }
        requestIds.remove(mcpId);
        processUserId.remove(mcpId);
    }

    public List<Map<String, Object>> listTools(Long userId, Long mcpId) {
        if (!runningProcesses.containsKey(mcpId)) {
            startMcp(userId, mcpId);
        }

        try {
            Map<String, Object> request = createJsonRpcRequest("tools/list", new HashMap<>());
            Map response = sendJsonRpc(mcpId, request);

            List<Map<String, Object>> tools = new ArrayList<>();
            if (response != null && response.get("result") != null) {
                Map result = (Map) response.get("result");
                if (result.get("tools") != null) {
                    List toolList = (List) result.get("tools");
                    for (Object tool : toolList) {
                        Map toolMap = (Map) tool;
                        Map<String, Object> toolInfo = new HashMap<>();
                        toolInfo.put("mcpId", mcpId);
                        toolInfo.put("name", toolMap.get("name"));
                        toolInfo.put("description", toolMap.get("description"));
                        toolInfo.put("inputSchema", toolMap.get("inputSchema"));
                        tools.add(toolInfo);
                    }
                }
            }
            return tools;
        } catch (Exception e) {
            throw new RuntimeException("获取MCP工具列表失败: " + e.getMessage());
        }
    }

    public Map<String, Object> callTool(Long userId, Long mcpId, String toolName, Map<String, Object> arguments) {
        if (!runningProcesses.containsKey(mcpId)) {
            startMcp(userId, mcpId);
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("name", toolName);
            params.put("arguments", arguments != null ? arguments : new HashMap<>());

            Map<String, Object> request = createJsonRpcRequest("tools/call", params);
            Map response = sendJsonRpc(mcpId, request);

            if (response != null && response.get("result") != null) {
                return (Map<String, Object>) response.get("result");
            }
            return Collections.singletonMap("error", "工具调用无响应");
        } catch (Exception e) {
            throw new RuntimeException("调用MCP工具失败: " + e.getMessage());
        }
    }

    public boolean isRunning(Long mcpId) {
        Process process = runningProcesses.get(mcpId);
        return process != null && process.isAlive();
    }

    @PreDestroy
    public void shutdown() {
        runningProcesses.keySet().forEach(id -> stopMcp(0L, id));
    }

    private void checkQuotaAndThrow(Long userId) {
        Map<String, Object> quota = checkQuota(userId);
        if (!(Boolean) quota.get("canEnable")) {
            throw new RuntimeException("已达到最大同时运行MCP数量限制");
        }
    }

    private List<String> buildCommand(McpServer server) {
        List<String> command = new ArrayList<>();
        command.add(server.getCommand());

        if (server.getArgs() != null && !server.getArgs().isEmpty()) {
            try {
                List argsList = objectMapper.readValue(server.getArgs(), List.class);
                command.addAll(argsList);
            } catch (Exception e) {
                throw new RuntimeException("args解析失败");
            }
        }
        return command;
    }

    private Map<String, Object> createJsonRpcRequest(String method, Map<String, Object> params) {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", method);
        request.put("params", params);
        return request;
    }

    private Map sendJsonRpc(Long mcpId, Map<String, Object> request) throws Exception {
        OutputStream writer = processWriters.get(mcpId);
        BufferedReader reader = processReaders.get(mcpId);

        if (writer == null || reader == null) {
            throw new RuntimeException("MCP进程未启动");
        }

        String jsonRequest = objectMapper.writeValueAsString(request);
        writer.write((jsonRequest + "\n").getBytes(StandardCharsets.UTF_8));
        writer.flush();

        String responseLine = reader.readLine();
        if (responseLine != null && !responseLine.isEmpty()) {
            return objectMapper.readValue(responseLine, Map.class);
        }
        return null;
    }
}
