package com.guga.walletserviceapi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.guga.walletserviceapi.logging.LogMarkers;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StartupReportTests {
    private static final Logger LOGGER = LogManager.getLogger(StartupReportTests.class);

    @LocalServerPort
    private int port;

    @Value("${spring.application.name:}")
    private  String SPRING_APPLICATION_NAME;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void printStartupReport() {

        String uri = "http://localhost:" + port + "/" + SPRING_APPLICATION_NAME;

        LOGGER.info(LogMarkers.LOG, "/actuator >> " + rest.getForEntity(uri + "/actuator", String.class).getStatusCode());
        LOGGER.info(LogMarkers.LOG, "/actuator/health >> " + rest.getForEntity(uri + "/actuator/health", String.class).getStatusCode());

        String url = uri + "/actuator/startup";
        ResponseEntity<String> resp = rest.getForEntity(url, String.class);

        LOGGER.info(LogMarkers.LOG, "=== STARTUP REPORT ===");
        LOGGER.info(LogMarkers.LOG, "URI: " + url);
        LOGGER.info(LogMarkers.LOG, "Status: " + resp.getStatusCode());
        LOGGER.info(LogMarkers.LOG, "Body length: " + (resp.getBody() == null ? 0 : resp.getBody().length()));
        LOGGER.info(LogMarkers.LOG, resp.getBody());
        LOGGER.info(LogMarkers.LOG, "======================");
    }
}