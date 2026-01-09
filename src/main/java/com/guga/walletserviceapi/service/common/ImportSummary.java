package com.guga.walletserviceapi.service.common;

public record ImportSummary(
    int recordsCreated,
    String status,
    String message
) {
    public ImportSummary(int recordsCreated, String status, String message) {
        this.recordsCreated = recordsCreated;
        this.status = status;
        this.message = message;
    }
}