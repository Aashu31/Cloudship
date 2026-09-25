package com.cloudship.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    @Value("${cloudship.name:CloudShip}")
    private String name;

    @Value("${cloudship.version:8.0.0}")
    private String version;

    @Value("${cloudship.phase:Version 8 — Observability, Monitoring & Zero-Trust Control}")
    private String phase;

    @Value("${cloudship.environment:Production}")
    private String environment;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getVersion() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("version", version);
        body.put("displayVersion", "v" + version);
        body.put("phase", phase);
        body.put("environment", environment);
        body.put("status", "UP");
        body.put("timestamp", OffsetDateTime.now().toString());

        return ResponseEntity.ok(body);
    }
}
