package com.guga.walletserviceapi.dto.auth;

public record TokenResponse(String accessToken, String refreshToken) { }