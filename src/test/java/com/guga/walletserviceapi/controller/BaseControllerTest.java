package com.guga.walletserviceapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.SecretKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.header.Header;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.config.IBaseControllerTest;
import com.guga.walletserviceapi.config.OpenApiConfig;
import com.guga.walletserviceapi.dto.auth.LoginRequest;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.RandomMock;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Routers;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.security.filter.JwtAuthenticationFilter;
import com.guga.walletserviceapi.security.handler.CustomAccessDeniedHandler;
import com.guga.walletserviceapi.security.handler.CustomAuthenticationEntryPoint;
import com.guga.walletserviceapi.security.jwt.JwtService;
import com.guga.walletserviceapi.seeder.SeedExecutor;
import com.guga.walletserviceapi.seeder.SeedOrderConfig;
import com.guga.walletserviceapi.seeder.SeedRunner;
import com.guga.walletserviceapi.service.LoginAuthService;
import com.guga.walletserviceapi.service.common.DataPersistenceService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.NonNull;

@Import({
    com.guga.walletserviceapi.config.ConfigProperties.class,
    com.guga.walletserviceapi.config.PasswordConfig.class,
    com.guga.walletserviceapi.service.common.DataPersistenceService.class,
    com.guga.walletserviceapi.config.SecurityConfig.class,
    com.guga.walletserviceapi.helpers.TransactionUtilsMock.class
    }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public abstract class BaseControllerTest implements IBaseControllerTest {

    protected static final Logger LOGGER = LogManager.getLogger(BaseControllerTest.class);
    protected static final boolean CREATE_JSON_MOCKS = true;

    @Autowired
    protected WebApplicationContext context;    
    //protected ApplicationContext context;

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

    @MockitoBean
    protected org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    protected DataPersistenceService dataPersistenceService;

    @Autowired
    protected ObjectMapper objectMapper = FileUtils.instanceObjectMapper();

    @MockitoBean
    protected OpenApiConfig openApiConfig;

    @Value("${spring.application.name:}")
    protected String CONTEXT_PATH;

    @Value("${jwt.secret:mock_jwt_secret}")
    protected String MOCK_JWT_SECRET;

    @Value("${jwt.secret:mock_jwt_secret}.refresh")
    protected String MOCK_JWT_SECRET_REFRESH = MOCK_JWT_SECRET + ".refresh";

    @Value("${WALLET_USER:wallet_user}")
    protected String MOCK_WALLET_USER;

    @Value("${WALLET_PASS:wallet_pass}")
    protected String MOCK_WALLET_PASS;

    @Value("${app.seeder.enabled:false}") 
    protected boolean seederEnabled; 

    //protected String URI_API;
    protected List<ParamApp> paramsApp;
    protected List<Wallet> wallets;
    protected List<Customer> customers;
    protected List<Transaction> transactions;
    protected List<DepositSender> depositSenders;
    protected List<LoginAuth> loginAuths;
    protected List<MovementTransaction> movements;

    protected JwtAuthenticationDetails jwtAuthDetails;
    //protected LoginAuth loginAuthMock;
    //protected LoginRequest loginRequestMock; 

    protected static boolean useContextPath = false;
    protected static boolean useApiPrefix = false;

    protected org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors mockPostProcessors;
    protected SecretKey key;
    protected String bearerAuth;

    @Autowired
    protected TransactionUtilsMock transactionUtilsMock;

    @BeforeAll
    void setupOnce() {
        // 1. Inicializa a Key para o generateAccessToken não dar erro
        // Usamos a String MOCK_JWT_SECRET para criar uma SecretKey válida
        this.key = Keys.hmacShaKeyFor(MOCK_JWT_SECRET.getBytes(StandardCharsets.UTF_8));

        loadMockData();
        runDatabaseSeeder();
    }

    @BeforeEach
    void setup() {
        //Mockito.reset(this.jwtAuthenticationFilter, this.jwtService, this.authenticationManager);

        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        this.mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
                .alwaysDo(print())
                .build();
    }

    protected void runDatabaseSeeder() {
        if (seederEnabled) {
            LOGGER.info(LogMarkers.LOG, "Seeding database for test...");            
            
            SeedOrderConfig config = new SeedOrderConfig();
            SeedExecutor executor = new SeedExecutor(context);
            SeedRunner runner = new SeedRunner(executor, config);
            runner.runSeed();
            LOGGER.info(LogMarkers.LOG, "Processo Seed finalizado.");
        }
        else {
            LOGGER.info(LogMarkers.LOG, "Do not exec seeder database for test [seederEnabled=false]...");
        }
    }

    // private void encriptAccessKeyLoginAuthListMock(List<LoginAuth> loginAuths) {
    //     for (LoginAuth loginAuth : loginAuths) {
    //         String rawAccessKey = loginAuth.getAccessKey();
    //         String encodedAccessKey = passwordEncoder.encode(rawAccessKey);
    //         loginAuth.setAccessKey(encodedAccessKey);
    //     }
    // }

    protected String getBaseUri(String api) {
        if (useApiPrefix) api = Routers.PREFIX + api;
        if (useContextPath) api = Routers.CONTEXT_PATH + api;
        return api.replace("//", "/");
    }

    protected LoginAuth setupMockAuth(List<LoginRole> roles) {
        LoginAuth loginAuthMock = null;
        if (RandomMock.getDynamicInt10(6)) {
            loginAuthMock = getLogin(new LoginRequest(this.MOCK_WALLET_USER, this.MOCK_WALLET_PASS));
        } else {
            loginAuthMock = getRandomLoginByRole(roles);
        }
        
        autenticarLogin(loginAuthMock);
        
        return loginAuthMock;
    }

    protected void autenticarLogin(LoginAuth loginAuth) {
        bearerAuth = generateAccessToken(loginAuth);
        autenticatorLoginMock(loginAuth);
        setJwtAndContextSecurity(loginAuth);
    }

    protected LoginAuth getLogin(LoginRequest login) {
        return loginAuths.stream()
            .filter(Objects::nonNull)
            .filter(la -> la.getLogin().toLowerCase().equals(login.username().toLowerCase()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + login.username()));
    }

    protected LoginAuth getRandomLoginByRole(List<LoginRole> roles) {
        List<LoginAuth> eligibleLogins = loginAuths.stream()
            .filter(Objects::nonNull)
            .filter(la -> la.getRole().stream().anyMatch(roles::contains))
            .toList();
        if (eligibleLogins.isEmpty()) {
            throw new RuntimeException("Nenhum LoginAuth encontrado para as roles: " + roles);
        }
        int index = ThreadLocalRandom.current().nextInt(eligibleLogins.size());
        return eligibleLogins.get(index);
    }

    /**
     * NICHO JWT: Prepara o contexto de segurança simulando a validação de um Token.
     * Use este método para testar endpoints protegidos por @PreAuthorize.
     */
    protected void setJwtAndContextSecurity(LoginAuth loginAuth) {
        // Configura o Mock do JwtService (Nicho JWT) para reconhecer o token gerado
        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(jwtService.extractLogin(anyString())).thenReturn(loginAuth.getLogin());
        
        // Simula os serviços auxiliares
        when(loginAuthService.findByLogin(loginAuth.getLogin())).thenReturn(loginAuth);
        when(jwtService.generateAccessToken(loginAuth)).thenReturn(bearerAuth);
        when(jwtService.generateRefreshToken(loginAuth)).thenReturn(bearerAuth + ".refresh");
    }

    /**
     * NICHO USERDETAILS: Simula o processo de autenticação via Login.
     * Use este método especificamente para testar o endpoint /login.
     */
    protected void autenticatorLoginMock(LoginAuth loginAuth) {
        List<SimpleGrantedAuthority> grantedAuthorities = loginAuth.getRole().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .toList();

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            loginAuth.getLogin(), 
            loginAuth.getAccessKey(), 
            grantedAuthorities
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities()
        );
        // Sem isso, o @PreAuthorize falha porque não acha ninguém logado
        SecurityContextHolder.getContext().setAuthentication(authentication);


        jwtAuthDetails = JwtAuthenticationDetails.builder()
            .customerId(loginAuth.getCustomerId())
            .walletId(loginAuth.getWalletId())
            .login(loginAuth.getLogin())
            .roles(loginAuth.getRole())
            .loginType(loginAuth.getLoginAuthType().toString())
            .loginId(loginAuth.getId())
            .build();
        when(authUserProvider.get()).thenReturn(jwtAuthDetails);

        // Simula o AuthenticationManager (Nicho UserDetails)
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
    }

    public MockHttpServletRequest mockHttpServletRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.1");
        request.setRemoteHost("localhost");
        request.setServerName(CONTEXT_PATH);
        return request;
    }

    public String generateAccessToken(LoginAuth loginAuth) {
        long ACCESS_TOKEN_VALIDITY = 30 * 60 * 1000; 
        Instant now = Instant.now()
                        .atZone(ZoneId.of("America/Sao_Paulo"))
                        .toInstant();
                        
        return Jwts.builder()            
            .subject(loginAuth.getLogin())
            .claim("loginId", loginAuth.getId())
            .claim("login", loginAuth.getLogin())
            .claim("customerId", loginAuth.getCustomerId())
            .claim("loginAuthType", loginAuth.getLoginAuthType())
            .claim("walletId", loginAuth.getWalletId())
            .claim("roles", loginAuth.getRole())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(ACCESS_TOKEN_VALIDITY)))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }

    protected ResultActions performRequest(
        @NonNull HttpMethod method, 
        @NonNull String uri, 
        Object content,
        MultiValueMap<String, String> params,
        List<Header> headers) throws Exception {

        Objects.requireNonNull(method, "O método HTTP não pode ser nulo");
        Objects.requireNonNull(uri, "A URI não pode ser nula");

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.request(method, getBaseUri(uri))
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                ;
        
        if (headers != null && !headers.isEmpty()) {
            for (Header header : headers) {
                requestBuilder.header(header.getName(), header.getValues());
            }
        }
        
        // Adiciona o Header APENAS se o token existir
        if (bearerAuth != null && !bearerAuth.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + bearerAuth);
        }

        // Adiciona parâmetros de query se existirem (?page=1&size=10)
        if (params != null && !params.isEmpty()) {
            requestBuilder.params(params);
        }

        if (content != null) {
            String contentString = objectMapper.writeValueAsString(content);
            requestBuilder.content(contentString);
        }

        return mockMvc.perform(requestBuilder);

    }

/**
     * Método centralizador para chamadas MockMvc
     */
    @SuppressWarnings("null")
    protected ResultActions performRequest(
        @NonNull HttpMethod method, 
        @NonNull String uri, 
        Object content,
        MultiValueMap<String, String> params) throws Exception {

        return performRequest(method, uri, content, params, null);

    }

    protected MultiValueMap<String, String> params(String... pairs) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.add(pairs[i], pairs[i + 1]);
        }
        return map;
    }

}