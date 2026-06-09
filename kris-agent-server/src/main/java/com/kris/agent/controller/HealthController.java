package com.kris.agent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ResponseEntity health() {
        return ResponseEntity.ok(Collections.singletonMap("ok", true));
    }
}
