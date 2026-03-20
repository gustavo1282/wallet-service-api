package com.guga.walletserviceapi.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guga.walletserviceapi.dto.webhook.AlertmanagerWebhook;
import com.guga.walletserviceapi.service.AlertProcessor;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/alerts")
@AllArgsConstructor
public class AlertWebhookController {
    
    private static final Logger LOGGER = LogManager.getLogger(AlertWebhookController.class);

    private final AlertProcessor processor;

    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(@RequestBody AlertmanagerWebhook payload) {
        LOGGER.warn("AlertmanagerWebhook [payload{}]", payload);
        processor.process(payload);
        return ResponseEntity.ok().build();
    }    

}
