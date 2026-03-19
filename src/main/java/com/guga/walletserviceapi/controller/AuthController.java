package com.guga.walletserviceapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.guga.walletserviceapi.dto.auth.LoginRequest;
import com.guga.walletserviceapi.dto.auth.TokenResponse;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.security.jwt.JwtService;
import com.guga.walletserviceapi.service.LoginAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Operation(
        operationId = "auth_01_login",
        summary = "Login",
        description = "Authenticates the user and returns accessToken and refreshToken."
        )
    @PostMapping("/login")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = LoginRequest.class),
            examples = @ExampleObject(
                name = "Standard User Login",
                value = "{\"username\": \"user@example.com\", \"password\": \"yourpassword\"}"
            )
        )
    )
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {

        // 1. Autentica o usuário (valida username/password) usando o Spring Security
        //    É crucial passar a senha original da requisição (request.password()), não a senha criptografada.
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
            //new UsernamePasswordAuthenticationToken(jwtAuthDetails, null, authorities);
        );

        // 2. Se a autenticação for bem-sucedida, coloca o usuário no contexto de segurança
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Busca o usuário completo (com todos os campos da entidade) para gerar o token
        //    O "Principal" aqui é o UserDetails retornado pelo seu LoginAuthService.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        LoginAuth loginAuth = loginAuthService.findByLogin(userDetails.getUsername());

        // 4. Gera os tokens para o usuário autenticado
        String accessToken = jwtService.generateAccessToken(loginAuth);
        String refreshToken = jwtService.generateRefreshToken(loginAuth);

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

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
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
 
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(
        operationId = "auth_04_register",
        summary = "Register",
        description = "Registers a new user."
    )
    @PostMapping("/register")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = LoginRequest.class),
            examples = @ExampleObject(
                name = "New User Registration",
                value = "{\"username\": \"newuser\", \"password\": \"strongpassword\"}"
            )
        )
    )
    public ResponseEntity<LoginAuth> register(@Valid @RequestBody LoginRequest request) {
        LoginAuth loginAuth = loginAuthService.register(request.username(), request.password());
        return ResponseEntity.ok( loginAuth );
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(
        operationId = "auth_01_any_login",
        summary = "Any Login",
        description = "Authenticates a user based on the environment profile. For test/dev environments, it can return a random user. Otherwise, it performs standard authentication. This endpoint does NOT use the standard Spring AuthenticationManager."
    )
    @PostMapping("/test/anylogin")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = LoginRequest.class),
            examples = @ExampleObject(
                name = "Exemplo Login Teste",
                value = "{\"username\": \"anyuser\", \"password\": \"anypassword\"}"
            )
        )
    )
    public ResponseEntity<LoginAuth> anyLogin(@Valid @RequestBody LoginRequest request) {


        boolean isAnyUser = (request.username().equals("anyuser") && 
                            request.password().equals("anypassword"));

        if (!isAnyUser) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        LoginAuth loginAuth = LoginAuth.builder()
            .login("anyuser")
            .accessKey("anypassword")
            .build();

        return ResponseEntity.ok(loginAuth);
    }

}