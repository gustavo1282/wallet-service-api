package com.guga.walletserviceapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.controller.AuthController.LoginRequest;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.enums.LoginAuthType;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.security.jwt.JwtService;
import com.guga.walletserviceapi.service.LoginAuthService;

@ActiveProfiles("test")
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Mocks das Dependências Diretas do Controller ---
    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private LoginAuthService loginAuthService;

    @MockitoBean
    private JwtAuthenticatedUserProvider authUserProvider;

    // --- Variáveis de Ambiente ---
    @Value("${controller.path.base}")
    private String BASE_PATH;

    private String URI_API;
    private static final String MOCK_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.mockAccess";
    private static final String MOCK_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiJ9.mockRefresh";
    private static final String TEST_USER = "wallet_user";
    private static final String TEST_PASS = "wallet_pass";

    private static final String API_NAME = "/auth";

    @BeforeAll
    void setUpAll() {
        URI_API = BASE_PATH + API_NAME;
    }

    // =========================================================
    // CONTEXTO DE AUTENTICAÇÃO (Login, Register, Refresh)
    // =========================================================

    @Nested
    @DisplayName("Operações de Autenticação (Públicas)")
    class AuthenticationContext {

        @Test
        @DisplayName("Deve realizar login com sucesso e retornar tokens")
        void login_ok() throws Exception {
            // 1. Arrange
            LoginRequest request = new LoginRequest(TEST_USER, TEST_PASS);
            LoginAuth loginAuthMock = createLoginAuthMock();

            // Simula autenticação bem sucedida pelo Spring Security
            Authentication auth = new UsernamePasswordAuthenticationToken(
                TEST_USER, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

            when(loginAuthService.findByLogin(request.username()))
                .thenReturn(loginAuthMock);

            when(jwtService.generateAccessToken(loginAuthMock)).thenReturn(MOCK_ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(loginAuthMock)).thenReturn(MOCK_REFRESH_TOKEN);

            // 2. Act & Assert
            mockMvc.perform(post(URI_API + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(MOCK_ACCESS_TOKEN))
                    .andExpect(jsonPath("$.refreshToken").value(MOCK_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("Deve registrar novo usuário e retornar dados do LoginAuth")
        void register_ok() throws Exception {
            // 1. Arrange
            LoginRequest request = new LoginRequest(TEST_USER, TEST_PASS);
            LoginAuth createdAuth = createLoginAuthMock();
            
            when(loginAuthService.register(request.username(), request.password()))
                .thenReturn(createdAuth);

            // 2. Act & Assert
            mockMvc.perform(post(URI_API + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value(TEST_USER))
                    .andExpect(jsonPath("$.accessKey").exists());
        }

        @Test
        @DisplayName("Deve renovar token (Refresh) com sucesso")
        void refreshToken_ok() throws Exception {
            // 1. Arrange
            String oldRefreshToken = MOCK_REFRESH_TOKEN;
            String newAccessToken = "new.mock.access.token";
            LoginAuth loginAuthMock = createLoginAuthMock();

            when(jwtService.validateToken(oldRefreshToken)).thenReturn(true);
            when(jwtService.extractLogin(oldRefreshToken)).thenReturn(TEST_USER);
            when(loginAuthService.findByLogin(TEST_USER)).thenReturn(loginAuthMock);
            when(jwtService.generateAccessToken(loginAuthMock)).thenReturn(newAccessToken);

            // 2. Act & Assert
            // Nota: O endpoint espera @RequestParam, então usamos .param()
            mockMvc.perform(post(URI_API + "/refresh")
                    .param("refreshToken", oldRefreshToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(newAccessToken))
                    .andExpect(jsonPath("$.refreshToken").value(oldRefreshToken));
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request se refresh token for inválido")
        void refreshToken_badRequest() throws Exception {
            when(jwtService.validateToken(anyString())).thenReturn(false);

            mockMvc.perform(post(URI_API + "/refresh")
                    .param("refreshToken", "invalid_token"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================
    // CONTEXTO DE INFORMAÇÕES DO USUÁRIO
    // =========================================================

    @Nested
    @DisplayName("Dados do Usuário Autenticado")
    class UserContext {

        @Test
        @DisplayName("Deve retornar os detalhes da autenticação atual")
        void getDataLogin_ok() throws Exception {
            // 1. Arrange
            JwtAuthenticationDetails details = JwtAuthenticationDetails.builder()
                .login(TEST_USER)
                .customerId(100L)
                .roles(List.of(LoginRole.USER))
                .build();

            when(authUserProvider.get()).thenReturn(details);

            // 2. Act & Assert
            mockMvc.perform(get(URI_API + "/data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value(TEST_USER))
                    .andExpect(jsonPath("$.customerId").value(100));
        }
    }

    // =========================================================
    // AUXILIARES
    // =========================================================

    private LoginAuth createLoginAuthMock() {
        return LoginAuth.builder()
            .id(1L)
            .customerId(100L)
            .login(TEST_USER)
            .accessKey(UUID.randomUUID().toString()) // Simula senha criptografada
            .loginAuthType(LoginAuthType.USER_NAME)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}