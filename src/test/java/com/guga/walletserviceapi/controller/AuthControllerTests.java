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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.fasterxml.jackson.core.type.TypeReference;
import com.guga.walletserviceapi.controller.AuthController.LoginRequest;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.LoginAuthType;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;

@WebMvcTest(AuthController.class)
@Import({
    com.guga.walletserviceapi.config.ConfigProperties.class,
    com.guga.walletserviceapi.config.PasswordConfig.class,
    com.guga.walletserviceapi.service.common.DataPersistenceService.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests extends BaseControllerTest {

    private static final String API_NAME = "/auth";

    @BeforeAll
    void setupOnce() {
        this.URI_API = getBaseUri(API_NAME);
        loadMockData();
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(
            this.jwtAuthenticationFilter,            
            this.jwtService,
            this.authenticationManager
        );
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
            LoginRequest request = new LoginRequest(MOCK_USER_NAME, MOCK_USER_PASS);
            LoginAuth loginAuthMock = createLoginAuthMock();

            // Simula autenticação bem sucedida pelo Spring Security
            Authentication auth = new UsernamePasswordAuthenticationToken(
                MOCK_USER_NAME, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

            when(loginAuthService.findByLogin(request.username()))
                .thenReturn(loginAuthMock);

            when(jwtService.generateAccessToken(loginAuthMock)).thenReturn(MOCK_JWT_SECRET);
            when(jwtService.generateRefreshToken(loginAuthMock)).thenReturn(MOCK_JWT_SECRET_REFRESH);

            // 2. Act & Assert
            mockMvc.perform(post(URI_API + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(MOCK_JWT_SECRET))
                    .andExpect(jsonPath("$.refreshToken").value(MOCK_JWT_SECRET_REFRESH));
        }

        @Test
        @DisplayName("Deve registrar novo usuário e retornar dados do LoginAuth")
        void register_ok() throws Exception {
            // 1. Arrange
            LoginRequest request = new LoginRequest(MOCK_USER_NAME, MOCK_USER_PASS);
            LoginAuth createdAuth = createLoginAuthMock();
            
            when(loginAuthService.register(request.username(), request.password()))
                .thenReturn(createdAuth);

            loginAuths.add(createdAuth);

            // 2. Act & Assert
            mockMvc.perform(post(URI_API + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value(MOCK_USER_NAME))
                    .andExpect(jsonPath("$.accessKey").exists());
        }

        @Test
        @DisplayName("Deve renovar token (Refresh) com sucesso")
        void refreshToken_ok() throws Exception {
            // 1. Arrange
            String oldRefreshToken = MOCK_JWT_SECRET_REFRESH;
            String newAccessToken = "new.mock.access.token";
            LoginAuth loginAuthMock = createLoginAuthMock();

            when(jwtService.validateToken(oldRefreshToken)).thenReturn(true);
            when(jwtService.extractLogin(oldRefreshToken)).thenReturn(MOCK_USER_NAME);
            when(loginAuthService.findByLogin(MOCK_USER_NAME)).thenReturn(loginAuthMock);
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
                .login(MOCK_USER_NAME)
                .customerId(100L)
                .roles(List.of(LoginRole.USER))
                .build();

            when(authUserProvider.get()).thenReturn(details);

            // 2. Act & Assert
            mockMvc.perform(get(URI_API + "/data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value(MOCK_USER_NAME))
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
            .login(MOCK_USER_NAME)
            .accessKey(UUID.randomUUID().toString()) // Simula senha criptografada
            .loginAuthType(LoginAuthType.USER_NAME)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Override
    public void loadMockData() {
        paramsApp = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP, new TypeReference<List<ParamApp>>() {});
        customers = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER, new TypeReference<List<Customer>>() {});      
        wallets = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET, new TypeReference<List<Wallet>>() {});
        loginAuths = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH, new TypeReference<List<LoginAuth>>() {});
    }
}