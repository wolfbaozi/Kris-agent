package com.kris.agent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 健康检查控制器
 *
 * 【前端类比】相当于前端心跳检测接口，用于负载均衡器或监控系统判断服务是否存活
 * 返回 { ok: true } 表示服务正常运行
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ResponseEntity health() {
        return ResponseEntity.ok(Collections.singletonMap("ok", true));
    }
}
