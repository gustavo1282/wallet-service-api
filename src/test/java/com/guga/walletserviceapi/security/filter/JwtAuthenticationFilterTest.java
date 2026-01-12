package com.guga.walletserviceapi.security.filter;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.guga.walletserviceapi.security.jwt.JwtService;
import com.guga.walletserviceapi.service.LoginAuthService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginAuthService loginAuthService;

    @Test
    void quandoNaoHaToken_naoAutentica() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void quandoTokenInvalido_naoAutentica() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token_invalido");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(jwtService.validateToken("token_invalido")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

