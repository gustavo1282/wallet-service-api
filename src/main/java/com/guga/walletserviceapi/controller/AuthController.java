package com.guga.walletserviceapi.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.guga.walletserviceapi.security.JwtService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${controller.path.base}/auth")
@Tag(name = "Authenticator", description = "Endpoints for managing Authenticator")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager; // Requer bean configurado no SecurityConfig

    // DTO simples para login
    public static class LoginRequest {
        public String username;
        public String password;
    }

    // DTO simples para resposta
    public static class TokenResponse {
        public String accessToken;
        public String refreshToken;

        public TokenResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {

        // Usa o Spring Security AuthenticationManager para validar o usuário/senha
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username, request.password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtService.generateAccessToken(request.username);
        String refreshToken = jwtService.generateRefreshToken(request.password);

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody LoginRequest request) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "User registration logic pending implementation in a Service class.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestParam String refreshToken) {
        if (jwtService.validateToken(refreshToken)) {
            String username = jwtService.extractUsername(refreshToken);
            String newAccessToken = jwtService.generateAccessToken(username);
            return ResponseEntity.ok(new TokenResponse(newAccessToken, refreshToken));
        }
        return ResponseEntity.badRequest().build();
    }
}