package com.guga.walletserviceapi.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
// Importações estáticas para facilitar when/thenReturn
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.servlet.ServletException;

@SpringBootTest(properties = {
    // 1. A maneira mais segura: desabilita o Vault
    "spring.cloud.vault.enabled=false"
    
    // 2. Se a 1ª não funcionar, exclua explicitamente as classes que estão falhando
    // Note: Use a notação "chave=valor" dentro de 'properties' ou 'classes'
    //"spring.autoconfigure.exclude=org.springframework.cloud.vault.config.VaultAutoConfiguration," +
    //"org.springframework.cloud.vault.config.VaultHealthIndicatorConfiguration"
})
public class JwtAuthenticationFilterIntegrationTest {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // Substitui o JwtService real por um mock no contexto do teste
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    private static String LOGIN_USER = "wallet_user";
    private static String LOGIN_PASSWORD = "wallet_pass";
    
    private static final String ACCESS_TOKEN_MOCK = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3YWxsZXRfdXNlciIsInJvbGVzIjoiIiwiaWF0IjoxNzY1MzQ4MjE3LCJleHAiOjE3NjUzNTA5MTd9.LntEXuGHNc8y7Y4QRs59qnQ2SKe3wmiBMsHmwAef81o";
    private static final String REFRESH_TOKEN_MOCK = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3YWxsZXRfcGFzcyIsImlhdCI6MTc2NTM0ODIxNywiZXhwIjoxNzY1NTIxMDE3fQ.sbTo7ssrHlJmW3cEt3n-qGRmwWTwAMk698RYJdkxNhA";

    private static final Boolean LOGIN_TOKEN_STATUS = true;

    @BeforeEach
    void setup() {        

        when(jwtService.generateAccessToken(LOGIN_USER)).thenReturn(ACCESS_TOKEN_MOCK);
        when(jwtService.generateRefreshToken(LOGIN_PASSWORD)).thenReturn(REFRESH_TOKEN_MOCK);

        // Configura o comportamento do mock service
        when(jwtService.validateToken(anyString())).thenReturn(LOGIN_TOKEN_STATUS);
        when(jwtService.extractUsername(anyString())).thenReturn(LOGIN_USER);

        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getName()).thenReturn(LOGIN_USER);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mockAuthentication);

    }


    @Test
    public void testRequisicaoSemTokenNaoAutentica() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Não adiciona o cabeçalho Authorization
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // O filtro deve prosseguir para o próximo filtro, pois não há token
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verificações: Por exemplo, se o status não é 401, ou se o filterChain.doFilter foi chamado
    }
    
    @Test
    @WithMockUser // Adiciona um usuário mockado para testar partes que exigem auth (se necessário)
    public void testTokenValidoAutenticaComSucesso() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token_valido_mock");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // Execute o filtro
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // A partir daqui, o usuário "testuser" deve estar no contexto de segurança do Spring
    }
}