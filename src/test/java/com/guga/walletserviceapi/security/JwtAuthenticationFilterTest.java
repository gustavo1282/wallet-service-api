package com.guga.walletserviceapi.security;

// Importação estática para métodos 'when' e 'thenReturn'
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class JwtAuthenticationFilterTest {

    // Simula o serviço JWT
    @Mock
    private JwtService jwtService;

    // Injeta o mock acima no filtro que estamos testando
    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    public void setUp() {
        // Inicializa os mocks antes de cada teste
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testTokenValidoPassaPeloFiltro() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token_valido_mock");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // >>> CONFIGURAÇÃO DA SIMULAÇÃO (MOCKING) <<<
        // Quando validateToken for chamado com "token_valido_mock", retorne TRUE
        when(jwtService.validateToken("token_valido_mock")).thenReturn(true);
        
        // Quando extractUsername for chamado, retorne "testuser"
        when(jwtService.extractUsername("token_valido_mock")).thenReturn("testuser");

        // Execute o filtro
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verifique se o usuário foi colocado no contexto de segurança (logado)
        // Isso requer uma verificação mais profunda, mas mostra o conceito de simulação.
        // O teste deve focar em verificar se filterChain.doFilter foi chamado corretamente.
    }

    @Test
    public void testTokenInvalidoBloqueiaAcesso() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token_invalido_mock");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // >>> CONFIGURAÇÃO DA SIMULAÇÃO (MOCKING) <<<
        // Quando validateToken for chamado com "token_invalido_mock", retorne FALSE
        when(jwtService.validateToken("token_invalido_mock")).thenReturn(false);

        // Execute o filtro
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verifique se a resposta foi 401 Unauthorized (depende da sua implementação exata no filtro)
        // assertEquals(401, response.getStatus()); 
    }
}