package com.guga.walletserviceapi.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import com.guga.walletserviceapi.audit.AuditLogContext;
import com.guga.walletserviceapi.audit.AuditLogger;
import com.guga.walletserviceapi.dto.auth.LoginRequest;
import com.guga.walletserviceapi.dto.auth.TokenResponse;
import com.guga.walletserviceapi.logging.LogMarkers;
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

    private static final Logger LOGGER = LogManager.getLogger(AuthController.class);

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

        LOGGER.info(LogMarkers.LOG, "AUTH_LOGIN | username={}", request.username());

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        LoginAuth loginAuth = loginAuthService.findByLogin(userDetails.getUsername());

        String accessToken = jwtService.generateAccessToken(loginAuth);
        String refreshToken = jwtService.generateRefreshToken(loginAuth);

        AuditLogger.log(
            "AUTH_LOGIN [SUCCESS]",
            AuditLogContext.from(authUserProvider.get())
                .toBuilder()
                .info("loginId=" + loginAuth.getId())
                .build()
        );

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

        LOGGER.info(LogMarkers.LOG, "AUTH_GET_MY_PROFILE | username={}", authDetails.getLogin());

        AuditLogger.log(
            "AUTH_GET_MY_PROFILE",
            AuditLogContext.from(authDetails).toBuilder()
                .info("loginId=" + authDetails.getLoginId())
                .build()
        );

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
        LOGGER.info(LogMarkers.LOG, "AUTH_REFRESH | tokenReceived=true");

        if (!jwtService.validateToken(refreshToken)) {
            LOGGER.warn(LogMarkers.LOG, "AUTH_REFRESH_INVALID_TOKEN");
            AuditLogger.log(
                "AUTH_REFRESH [INVALID_TOKEN]",
                AuditLogContext.builder().info("reason=invalid_token").build()
            );
            return ResponseEntity.badRequest().build();
        }

        String username = jwtService.extractLogin(refreshToken);
        LoginAuth loginAuth = loginAuthService.findByLogin(username);
        String newAccessToken = jwtService.generateAccessToken(loginAuth);

        AuditLogger.log(
            "AUTH_REFRESH [SUCCESS]",
            AuditLogContext.from(authUserProvider.get())
                .toBuilder()
                .info("loginId=" + loginAuth.getId())
                .build()
        );

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
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG, "AUTH_REGISTER | admin={} username={}",
            auditCtx.getUsername(), request.username()
        );

        AuditLogger.log("AUTH_REGISTER [START]", auditCtx);

        LoginAuth loginAuth = loginAuthService.register(request.username(), request.password());

        AuditLogger.log(
            "AUTH_REGISTER [SUCCESS]",
            auditCtx.toBuilder().info("loginId=" + loginAuth.getId()).build()
        );

        return ResponseEntity.ok(loginAuth);
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

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG, "AUTH_ANY_LOGIN | admin={} requestedUser={}",
            auditCtx.getUsername(), request.username()
        );

        AuditLogger.log("AUTH_ANY_LOGIN [START]", auditCtx);

        boolean isAnyUser = (request.username().equals("anyuser") &&
                            request.password().equals("anypassword"));

        if (!isAnyUser) {
            LOGGER.warn(LogMarkers.LOG, "AUTH_ANY_LOGIN_UNAUTHORIZED | requestedUser={}", request.username());
            AuditLogger.log(
                "AUTH_ANY_LOGIN [UNAUTHORIZED]",
                auditCtx.toBuilder().info("requestedUser=" + request.username()).build()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        LoginAuth loginAuth = LoginAuth.builder()
            .login("anyuser")
            .accessKey("anypassword")
            .build();

        AuditLogger.log(
            "AUTH_ANY_LOGIN [SUCCESS]",
            auditCtx.toBuilder().info("generatedUser=" + loginAuth.getLogin()).build()
        );

        return ResponseEntity.ok(loginAuth);
    }

}
