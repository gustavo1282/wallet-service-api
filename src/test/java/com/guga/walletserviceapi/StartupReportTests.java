package com.guga.walletserviceapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StartupReportTests {

    @LocalServerPort
    private int port;

    @Value("${spring.application.name:}")
    private  String SPRING_APPLICATION_NAME;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void printStartupReport() {

        String uri = "http://localhost:" + port + "/" + SPRING_APPLICATION_NAME;

        System.out.println("/actuator >> " + rest.getForEntity(uri + "/actuator", String.class).getStatusCode());
        System.out.println("/actuator/health >> " + rest.getForEntity(uri + "/actuator/health", String.class).getStatusCode());

        String url = uri + "/actuator/startup";
        ResponseEntity<String> resp = rest.getForEntity(url, String.class);

        System.out.println("=== STARTUP REPORT ===");
        System.out.println("URI: " + url);
        System.out.println("Status: " + resp.getStatusCode());
        System.out.println("Body length: " + (resp.getBody() == null ? 0 : resp.getBody().length()));
        System.out.println(resp.getBody());
        System.out.println("======================");
    }
}