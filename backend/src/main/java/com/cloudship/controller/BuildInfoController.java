package com.cloudship.controller;

import com.cloudship.dto.BuildInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/build-info")
public class BuildInfoController {

    @Value("${spring.application.name:cloudship-backend}")
    private String applicationName;

    @Value("${cloudship.version:1.0.0}")
    private String version;

    @Value("${cloudship.environment:Local Dev}")
    private String environment;

    @Value("${cloudship.docker.image:cloudship/backend:1.0.0}")
    private String dockerImage;

    @GetMapping
    public ResponseEntity<BuildInfoResponse> getBuildInfo() {
        BuildInfoResponse response = new BuildInfoResponse(
                applicationName,
                version,
                environment,
                dockerImage,
                System.getProperty("java.version", "17"),
                "READY"
        );
        return ResponseEntity.ok(response);
    }
}
