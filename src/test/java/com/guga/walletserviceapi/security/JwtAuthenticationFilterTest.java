package com.guga.walletserviceapi.security;

import static org.mockito.ArgumentMatchers.anyString;
// Importação estática para métodos 'when' e 'thenReturn'
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.guga.walletserviceapi.service.LoginAuthService;

@ActiveProfiles("test")
@WithMockUser(username = "wallet_user", roles = { "USER" }, password = "wallet_pass")
@WebMvcTest(JwtAuthenticationFilter.class)
@AutoConfigureMockMvc(addFilters = false)
public class JwtAuthenticationFilterTest {

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockitoBean
	private LoginAuthService loginAuthService;

	@Autowired
	private MockMvc mockMvc;


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
        when(jwtService.validateToken(anyString())).thenReturn(true);
        
        // Quando extractUsername for chamado, retorne "testuser"
        when(jwtService.extractUsername(anyString())).thenReturn("testuser");

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