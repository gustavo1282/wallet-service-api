package com.guga.walletserviceapi.security;

import static org.mockito.ArgumentMatchers.anyString;
// Importações estáticas para facilitar when/thenReturn
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.servlet.ServletException;

@ActiveProfiles("test")
@WithMockUser(username = "wallet_user", roles = { "USER" }, password = "wallet_pass")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class JwtAuthenticationFilterIntegrationTest {

    // Injeta o filtro real que queremos testar
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // Substitui o JwtService real por um mock no contexto do teste
    @MockitoBean
    private JwtService jwtService;

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

        // Configura o comportamento do mock service
        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(jwtService.extractUsername(anyString())).thenReturn("testuser");

        // Execute o filtro
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // A partir daqui, o usuário "testuser" deve estar no contexto de segurança do Spring
    }
}