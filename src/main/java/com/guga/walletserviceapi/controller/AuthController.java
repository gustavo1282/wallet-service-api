package com.guga.walletserviceapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.security.jwt.JwtService;
import com.guga.walletserviceapi.service.LoginAuthService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    private AuthenticationManager authenticationManager;

    @Autowired
    private LoginAuthService loginAuthService;


    // DTO simples para login
    public record LoginRequest(
        String username,
        String password
    ) {} 

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
    public ResponseEntity<TokenResponse> login(
        @Valid @RequestBody LoginRequest request
        ) 
    {

        // Usa o Spring Security AuthenticationManager para validar o usuário/senha
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username, request.password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginAuth loginAuth = loginAuthService.findByLogin(request.username);

        String accessToken = jwtService.generateAccessToken(loginAuth);
        String refreshToken = jwtService.generateRefreshToken(loginAuth);

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody LoginRequest request) {
        LoginAuth loginAuth = loginAuthService.register(request.username, request.password);
        return ResponseEntity.ok( loginAuth );
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
        @RequestParam String refreshToken
        ) 
    {
        if (!jwtService.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().build();
        }

        String username = jwtService.extractLogin(refreshToken);

        // 2. Busca o LoginAuth completo no banco
        LoginAuth loginAuth = loginAuthService.findByLogin(username);

        // 3. Gera novo access token com claims completas
        String newAccessToken = jwtService.generateAccessToken(loginAuth);

        // 4. Retorna o novo access token + refresh antigo
        return ResponseEntity.ok(new TokenResponse(newAccessToken, refreshToken));
    }


    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/data/{loginId}")
     public ResponseEntity<TokenResponse> getDataLogin() 
    {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();

        loginAuthService.loadUserByUsername(loginId);

        return new ResponseEntity<>(null, HttpStatus.OK);
    }
 
}