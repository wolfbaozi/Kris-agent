package com.kris.agent.controller;

import com.kris.agent.dto.DebugRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.DebugService;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final DebugService debugService;

    public DebugController(DebugService debugService) {
        this.debugService = debugService;
    }

    @PostMapping("/skill")
    public void debugSkill(@RequestBody DebugRequest request, Authentication authentication,
                            HttpServletResponse response) throws Exception {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        debugService.debugSkill(principal.getId(), request, response);
    }

    @PostMapping("/mcp")
    public void debugMcp(@RequestBody DebugRequest request, Authentication authentication,
                          HttpServletResponse response) throws Exception {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        debugService.debugMcp(principal.getId(), request, response);
    }
}
