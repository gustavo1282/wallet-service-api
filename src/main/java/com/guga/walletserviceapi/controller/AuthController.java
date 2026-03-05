package com.guga.walletserviceapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.security.jwt.JwtService;
import com.guga.walletserviceapi.service.LoginAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authenticator", description = "Endpoints for managing Authenticator")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginAuthService loginAuthService;
    private final JwtAuthenticatedUserProvider authUserProvider;    

    // DTO simples para login
    public record LoginRequest(String username, String password) {} 

    public record TokenResponse(String accessToken, String refreshToken) { }

    @Operation(
        operationId = "auth_01_login",
        summary = "Login",
        description = "Authenticates the user and returns accessToken and refreshToken."
        )
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {

        LoginAuth loginAuth = loginAuthService.findByLogin(request.username);

        if (loginAuth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = jwtService.generateAccessToken(loginAuth);
        String refreshToken = jwtService.generateRefreshToken(loginAuth);

        // Usa o Spring Security AuthenticationManager para validar o usuário/senha
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username, request.password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);


        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(
        operationId = "auth_02_get_my_profile",
        summary = "Get My Profile",
        description = "Returns authentication details for the authenticated user."
    )
    @GetMapping("/my_profile")
     public ResponseEntity<JwtAuthenticationDetails> getMyProfile() {
        JwtAuthenticationDetails authDetails = authUserProvider.get();
        return new ResponseEntity<>(authDetails, HttpStatus.OK);
    }    

    @Operation(
        operationId = "auth_03_refresh",
        summary = "Refresh Token",
        description = "Validates refreshToken and returns a new accessToken."
    )
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestParam String refreshToken) {
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
 
    @Operation(
        operationId = "auth_04_register",
        summary = "Register",
        description = "Registers a new user."
    )
    @PostMapping("/register")
    public ResponseEntity<LoginAuth> register(@Valid @RequestBody LoginRequest request) {
        LoginAuth loginAuth = loginAuthService.register(request.username, request.password);
        return ResponseEntity.ok( loginAuth );
    }

}