package com.guga.walletserviceapi.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.core.type.TypeReference;
import com.guga.walletserviceapi.dto.auth.LoginRequest;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Routers;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.LoginRole;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests extends BaseControllerTest {


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
            // Cria um usuário de mock para a autenticação
            LoginAuth loginAuthMock = getRandomLoginByRole(List.of(LoginRole.ADMIN, LoginRole.USER));
            bearerAuth = generateAccessToken(loginAuthMock);
            autenticatorLoginMock(loginAuthMock);
            setJwtAndContextSecurity(loginAuthMock);

            LoginRequest loginRequest = generateLoginRequest(loginAuthMock);

            performRequest(HttpMethod.POST, Routers.AUTH + "/login", loginRequest, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.accessToken", containsString(bearerAuth)))
                .andExpect(jsonPath("$.refreshToken", containsString(bearerAuth)));

        }

        @Test
        @DisplayName("Deve retornar 401 Unauthorized para credenciais inválidas")
        void login_fail_badCredentials() throws Exception {
            // 1. Arrange
            LoginRequest loginDTO = new LoginRequest("wronguser", "wrongpassword");
            
            // Simula o AuthenticationManager lançando uma exceção
            when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Bad credentials") {});

            // 2. Act & Assert
            performRequest(HttpMethod.POST, Routers.AUTH + "/login", loginDTO, null)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve registrar novo usuário e retornar dados do LoginAuth")
        void register_ok() throws Exception {

            // 1. Arrange            
            LoginAuth createdAuth = setupMockAuth(List.of(LoginRole.ADMIN));

            LoginRequest request = generateLoginRequest(createdAuth);

            when(loginAuthService.findByLogin(anyString())).thenReturn(createdAuth);
            
            when(loginAuthService.register(request.username(), request.password()))
                .thenReturn(createdAuth);

            loginAuths.add(createdAuth);

            // 2. Act & Assert
            performRequest(HttpMethod.POST, Routers.AUTH + "/register", request, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(createdAuth.getLogin()))
                .andExpect(jsonPath("$.accessKey").exists());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request ao tentar registrar usuário existente")
        void register_fail_userAlreadyExists() throws Exception {

            setupMockAuth(List.of(LoginRole.ADMIN));
            LoginRequest request = new LoginRequest(MOCK_WALLET_USER, MOCK_WALLET_PASS);

            // Simula o serviço lançando uma exceção apropriada
            when(loginAuthService.register(request.username(), request.password()))
                .thenThrow(new ResourceBadRequestException("User already exists"));

            // 2. Act & Assert
            performRequest(HttpMethod.POST, Routers.AUTH + "/register", request, null)
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve renovar token (Refresh) com sucesso")
        void refreshToken_ok() throws Exception {

            // 1. Arrange            
            LoginAuth loginAuthMock = setupMockAuth(List.of(LoginRole.ADMIN, LoginRole.USER));

            // 1. Arrange
            String oldRefreshToken = MOCK_JWT_SECRET_REFRESH;
            String newAccessToken = generateAccessToken(loginAuthMock);
            
            when(jwtService.validateToken(oldRefreshToken)).thenReturn(true);
            when(jwtService.extractLogin(oldRefreshToken)).thenReturn(MOCK_WALLET_USER);
            when(loginAuthService.findByLogin(MOCK_WALLET_USER)).thenReturn(loginAuthMock);
            when(jwtService.generateAccessToken(loginAuthMock)).thenReturn(newAccessToken);

            performRequest(HttpMethod.POST, Routers.AUTH + "/refresh", null, params("refreshToken", oldRefreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newAccessToken))
                .andExpect(jsonPath("$.refreshToken").value(oldRefreshToken));

        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request se refresh token for inválido")
        void refreshToken_badRequest() throws Exception {

            setupMockAuth(List.of(LoginRole.ADMIN));
            when(jwtService.validateToken(anyString())).thenReturn(false);

            performRequest(HttpMethod.POST, Routers.AUTH + "/refresh", null, params("refreshToken", "invalid_token"))
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
        @DisplayName("Deve retornar os detalhes do perfil do usuário autenticado")
        void getMyProfile_ok() throws Exception {

            LoginAuth loginAuthMock = setupMockAuth(List.of(LoginRole.ADMIN, LoginRole.USER));

            // 2. Act & Assert
            performRequest(HttpMethod.GET, Routers.AUTH + "/my_profile", null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(loginAuthMock.getLogin()))
                .andExpect(jsonPath("$.customerId").value(loginAuthMock.getCustomerId()));
        }

        @Test
        @DisplayName("Deve retornar OK com corpo vazio se o provedor de autenticação falhar")
        void getMyProfile_whenProviderFails_shouldReturnOkWithEmptyBody() throws Exception {

            setupMockAuth(List.of(LoginRole.ADMIN, LoginRole.USER));

            // 1. Arrange
            when(authUserProvider.get()).thenReturn(null);

            // 2. Act & Assert
            performRequest(HttpMethod.GET, Routers.AUTH + "/my_profile", null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());
        }
    }

    // =========================================================
    // CONTEXTO DE LOGIN DE TESTE
    // =========================================================

    @Nested
    @DisplayName("Operações de AnyLogin (Específico para Testes)")
    class AnyLoginContext {

        @Test
        @DisplayName("Deve autenticar com sucesso usando anyLogin e retornar tokens")
        void anyLogin_ok() throws Exception {

            setupMockAuth(List.of(LoginRole.ADMIN));

            // 1. Arrange
            LoginRequest request = new LoginRequest("anyuser", "anypassword");

            // 2. Act & Assert
            performRequest(HttpMethod.POST, Routers.AUTH + "/test/anylogin", request, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").exists())
                .andExpect(jsonPath("$.login").isNotEmpty())
                .andExpect(jsonPath("$.login").value(request.username()));
        }

        @Test
        @DisplayName("Deve retornar 401 Unauthorized se anyLogin falhar")
        void anyLogin_fail() throws Exception {
            
            setupMockAuth(List.of(LoginRole.ADMIN));

            LoginRequest request = new LoginRequest("wronguser", "wrongpassword");

            // 2. Act & Assert
            performRequest(HttpMethod.POST, Routers.AUTH + "/test/anylogin", request, null)
                .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================
    // AUXILIARES
    // =========================================================

    // private LoginAuth createLoginAuthMock() {
    //     return loginAuthMock;
    // }

    @Override
    public void loadMockData() {
        paramsApp = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP, new TypeReference<List<ParamApp>>() {});
        customers = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER, new TypeReference<List<Customer>>() {});      
        wallets = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET, new TypeReference<List<Wallet>>() {});
        loginAuths = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH, new TypeReference<List<LoginAuth>>() {});
    }

    private LoginRequest generateLoginRequest(LoginAuth loginAuthMock) {
        return new LoginRequest(loginAuthMock.getLogin(), "password");
    }

}