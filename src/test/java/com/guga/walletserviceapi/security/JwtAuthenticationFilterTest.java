package com.guga.walletserviceapi.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
// Importação estática para métodos 'when' e 'thenReturn'
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;

import com.guga.walletserviceapi.service.LoginAuthService;

public class JwtAuthenticationFilterTest {

    @Mock
    private LoginAuthService loginAuthService; 

    // 2. Mock de outras dependências do filtro (como o serviço JWT)
    @Mock
    private JwtService jwtService;

    // Injeta o mock acima no filtro que estamos testando
    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static String LOGIN_USER = "wallet_user";
    private static String LOGIN_PASSWORD = "wallet_pass";
    private static final String ACCESS_TOKEN_MOCK = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3YWxsZXRfdXNlciIsInJvbGVzIjoiIiwiaWF0IjoxNzY1MzQ4MjE3LCJleHAiOjE3NjUzNTA5MTd9.LntEXuGHNc8y7Y4QRs59qnQ2SKe3wmiBMsHmwAef81o";
    private static final String REFRESH_TOKEN_MOCK = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3YWxsZXRfcGFzcyIsImlhdCI6MTc2NTM0ODIxNywiZXhwIjoxNzY1NTIxMDE3fQ.sbTo7ssrHlJmW3cEt3n-qGRmwWTwAMk698RYJdkxNhA";
    private static final Boolean LOGIN_TOKEN_STATUS = true;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // 1. Crie um Mock para o UserDetails
        UserDetails mockUserDetails = mock(UserDetails.class);

        // 2. Configure o UserDetails (mínimo necessário para o seu filtro não quebrar)
        when(mockUserDetails.getAuthorities()).thenReturn(Collections.emptyList()); // Ou a lista de authorities esperada
        when(mockUserDetails.getUsername()).thenReturn(LOGIN_USER);
        when(mockUserDetails.getPassword()).thenReturn(LOGIN_PASSWORD); 

        // 3. Configure o LoginAuthService para retornar este mock
        // O argumento do 'loadUserByUsername' pode ser anyString() se você não quiser checar o valor do username
        when(loginAuthService.loadUserByUsername(anyString())).thenReturn(mockUserDetails); 

        // (Opcional) Configure o JwtService, garantindo que o token seja "válido"
        when(jwtService.validateToken(anyString())).thenReturn(true); 
        when(jwtService.extractUsername(anyString())).thenReturn(LOGIN_USER);
    }

    @Test
    public void testTokenValidoPassaPeloFiltro() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + ACCESS_TOKEN_MOCK);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // >>> CONFIGURAÇÃO DA SIMULAÇÃO (MOCKING) <<<
        // Quando validateToken for chamado com "token_valido_mock", retorne TRUE
        when(jwtService.validateToken(ACCESS_TOKEN_MOCK)).thenReturn(LOGIN_TOKEN_STATUS);
        
        // Quando extractUsername for chamado, retorne "testuser"
        when(jwtService.extractUsername(ACCESS_TOKEN_MOCK)).thenReturn(LOGIN_USER);

        // Execute o filtro
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    }

    @Test
    public void testTokenInvalidoBloqueiaAcesso() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + ACCESS_TOKEN_MOCK);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // >>> CONFIGURAÇÃO DA SIMULAÇÃO (MOCKING) <<<
        // Quando validateToken for chamado com "token_invalido_mock", retorne FALSE
        when(jwtService.validateToken(ACCESS_TOKEN_MOCK)).thenReturn(LOGIN_TOKEN_STATUS);

        // Execute o filtro
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    }
}