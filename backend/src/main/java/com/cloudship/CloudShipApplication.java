package com.cloudship;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class CloudShipApplication {

    private static final Logger log = LoggerFactory.getLogger(CloudShipApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CloudShipApplication.class);
        Environment env = app.run(args).getEnvironment();
        log.info("==================================================================");
        log.info(" CloudShip Application [{}] started successfully!", env.getProperty("spring.application.name"));
        log.info(" Active Profile(s): {}", (Object) env.getActiveProfiles());
        log.info(" Server Port: {}", env.getProperty("server.port", "8080"));
        log.info(" Health Endpoint: http://localhost:{}/api/health", env.getProperty("server.port", "8080"));
        log.info("==================================================================");
    }
}
