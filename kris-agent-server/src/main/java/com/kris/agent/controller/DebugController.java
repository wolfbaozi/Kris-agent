package com.kris.agent.controller;

import com.kris.agent.dto.DebugRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.DebugService;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 调试控制器 —— Skill/MCP 在线调试（SSE 流式输出）
 *
 * 【前端类比】相当于前端"调试面板"功能的后端接口
 * 与 ChatController 类似，也是 SSE 流式响应，但直接操作 HttpServletResponse
 * 而不是返回 SseEmitter（两种方式都能实现 SSE，这里选择了更底层的方式）
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final DebugService debugService;

    public DebugController(DebugService debugService) {
        this.debugService = debugService;
    }

    /**
     * 调试 Skill POST /api/debug/skill
     * HttpServletResponse 直接操作响应流，手动写入 SSE 格式数据
     */
    @PostMapping("/skill")
    public void debugSkill(@RequestBody DebugRequest request, Authentication authentication,
                            HttpServletResponse response) throws Exception {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        debugService.debugSkill(principal.getId(), request, response);
    }

    /**
     * 调试 MCP POST /api/debug/mcp
     */
    @PostMapping("/mcp")
    public void debugMcp(@RequestBody DebugRequest request, Authentication authentication,
                          HttpServletResponse response) throws Exception {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        debugService.debugMcp(principal.getId(), request, response);
    }
}
