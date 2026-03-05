package com.guga.walletserviceapi.handler;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@io.swagger.v3.oas.annotations.media.Schema(name = "ErrorResponse")
public record ErrorResponse(
        int status,
        String error,
        String code,
        String message,
        String source,
        String path,
        String traceId,
        Instant timestamp
) {}