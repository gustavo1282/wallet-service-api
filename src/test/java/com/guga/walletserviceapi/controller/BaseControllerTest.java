package com.guga.walletserviceapi.controller;

import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.config.IBaseControllerTest;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.security.filter.JwtAuthenticationFilter;
import com.guga.walletserviceapi.security.jwt.JwtService;
import com.guga.walletserviceapi.service.LoginAuthService;
import com.guga.walletserviceapi.service.common.DataPersistenceService;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public abstract class BaseControllerTest implements IBaseControllerTest {

    protected static final Logger LOGGER = LogManager.getLogger(BaseControllerTest.class);

    protected static final boolean CREATE_JSON_MOCKS = true;

    @Autowired
    protected ApplicationContext context;

    @Autowired
    protected Environment env;

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected AuthenticationManager authenticationManager;

    @MockitoBean
    protected LoginAuthService loginAuthService;

    @MockitoBean
    protected JwtAuthenticatedUserProvider authUserProvider;

    @Autowired 
    protected org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    protected DataPersistenceService dataPersistenceService;

    //@Autowired
    //protected RequestMappingHandlerMapping handlerMapping;

    @Autowired
    protected ObjectMapper objectMapper = FileUtils.instanceObjectMapper();

    // --- Variáveis de Ambiente ---
    @Value("${spring.application.name:}")
    protected String CONTEXT_PATH;

    @Value("${app.api-prefix:}")
    protected String SERVLET_PATH;

    @Value("${jwt.secret:}")
    protected String MOCK_JWT_SECRET;
    
    @Value("${jwt.secret:mock_jwt_secret}.refresh")
    protected String MOCK_JWT_SECRET_REFRESH = MOCK_JWT_SECRET + ".refresh";
    
    @Value("${USER_NAME:wallet_user}")
    protected String MOCK_USER_NAME;
    
    @Value("${USER_PASS:wallet_pass}")
    protected String MOCK_USER_PASS;

    protected String URI_API;

    protected List<ParamApp> paramsApp;
    protected List<Wallet> wallets;
    protected List<Customer> customers;
    protected List<Transaction> transactions;
    protected List<DepositSender> depositSenders;
    protected List<LoginAuth> loginAuths;
    protected List<MovementTransaction> movements;

    protected String getBaseUri(String controllerPath) {
        // Garante que não haverá barras duplas ou faltando
        return (
            //CONTEXT_PATH + 
            SERVLET_PATH + 
            controllerPath).replace("//", "/");
    }

    // =========================================================
    // MÉTODOS UTILITÁRIOS (SETUP)
    // =========================================================

    protected LoginAuth setupMockAuth(List<LoginRole> roles) {
		LoginAuth login = getRandomLoginByRole(roles);

        JwtAuthenticationDetails details = JwtAuthenticationDetails.builder()
                .loginId(login.getId())
                .login(login.getLogin())
                .walletId(login.getWalletId())
                .customerId(login.getCustomerId())
                .roles(login.getRole())
                .build();

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, authorities)
        );

        // Garante que o provider injetado no Controller retorne o nosso mock
        when(authUserProvider.get()).thenReturn(details);
		return login;
    }


    private LoginAuth getRandomLoginByRole(List<LoginRole> roles) {

        //List<LoginRole> targetRoles = roles.stream()
        //    .map(r -> LoginRole.valueOf(r.trim().toUpperCase()))
        //    .toList();

        Set<Long> laWalletIds = wallets.stream()
            .map(Wallet::getWalletId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        boolean ignoreTransactions = (transactions == null);

        Set<Long> laTransactionsIds = (ignoreTransactions) ? Collections.emptySet() :
                transactions.stream()
                .map(Transaction::getWalletId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        return loginAuths.stream()
            .filter(Objects::nonNull)
            .filter(la -> la.getRole().stream().anyMatch(roles::contains))
            .filter(la -> laWalletIds.contains(la.getWalletId()))
            .filter(la -> (ignoreTransactions || laTransactionsIds.contains(la.getWalletId())))
            .findAny()
            .orElseThrow(() -> new RuntimeException(
                "Nenhum LoginAuth elegível encontrado para roles: " + roles)
            );
    }


    public enum LoginPickMode {
        BASIC,
        WITH_TRANSACTIONS
    }

}

