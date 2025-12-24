package com.guga.walletserviceapi.model.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Builder;
import lombok.Data;

@JsonPropertyOrder({
    "sessionId", "timeMillis", "traceId", "user", "operation", "component", "message"
})
@Data
@Builder
public class ApplicationLogContext {
    private String sessionId;
    private Long timeMillis;
    private String traceId;
    private String user;
    private String operation;
    private String component;
    private String message;
}
