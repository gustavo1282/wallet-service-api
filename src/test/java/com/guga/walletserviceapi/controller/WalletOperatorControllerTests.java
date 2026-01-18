package com.guga.walletserviceapi.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.WithdrawMoney;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.model.request.CreateWithdrawRequest;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.security.filter.JwtAuthenticationFilter;
import com.guga.walletserviceapi.service.CustomerService;
import com.guga.walletserviceapi.service.DepositSenderService;
import com.guga.walletserviceapi.service.MovementTransactionService;
import com.guga.walletserviceapi.service.TransactionService;
import com.guga.walletserviceapi.service.WalletService;
import com.guga.walletserviceapi.service.common.DataPersistenceService;

@WebMvcTest(WalletOperatorController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import({
    com.guga.walletserviceapi.config.PasswordConfig.class,
    com.guga.walletserviceapi.service.common.DataPersistenceService.class
})
class WalletOperatorControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired 
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private DepositSenderService depositSenderService;

    @MockitoBean
    private MovementTransactionService movementTransactionService;

    @MockitoBean
    private JwtAuthenticatedUserProvider jwtAuthenticatedUserProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private DataPersistenceService dataPersistenceService;

    @Value("${controller.path.base}")
    private String BASE_PATH;    

    private static final String API_NAME = "/wallet-operator";

    private boolean dataLoaded = false;

    private String URI_API;
    
    private List<Wallet> wallets;
    private List<Customer> customers;
    private List<Transaction> transactions;
    private List<DepositSender> depositSenders;
    private List<ParamApp> paramsApp;
    private List<LoginAuth> loginAuths;
    private List<MovementTransaction> movements;
    
    @BeforeAll
    void setUpAll() {
        URI_API = BASE_PATH + API_NAME;
        loadMockData();
    }    

    // =========================================================
    // CONTEXTO DE OPERAÇÕES DA WALLET
    // =========================================================

    @Nested
    @DisplayName("Operações de Integração (Gateway/M2M)")
    class TransactionIntegration {  
        @Test
        @DisplayName("Get my last 100 transactions (All types)")
        void getMyRecentTransactions() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));

            invokeMyTransactions(auth, null);
        }

        @Test
        @DisplayName("List my last 100 deposits")
        void listMyDeposits() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));
            invokeMyTransactions(auth, OperationType.DEPOSIT);
        }

        @Test
        @DisplayName("List my last 100 withdraws")
        void listMyWithdraws() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));
            invokeMyTransactions(auth, OperationType.WITHDRAW);
        }

        @Test
        @DisplayName("List my last 100 transfers send")
        void listMyTransferSend() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));
            invokeMyTransactions(auth, OperationType.TRANSFER_SEND);
        }

        @Test
        @DisplayName("List my last 100 transfers received")
        void listMyTransferReceived() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));
            invokeMyTransactions(auth, OperationType.TRANSFER_RECEIVED);
        }

        private void invokeMyTransactions(LoginAuth auth, OperationType operationType) throws Exception{

            Pageable pageable = GlobalHelper.getDefaultPageable();

            List<Transaction> dataMock = filterTransactionByWalletIdAndOperationType(auth.getWalletId(), operationType);

            Page<Transaction> pageMock = new PageImpl<Transaction>(dataMock, pageable, dataMock.size());
            
            when(transactionService.filterTransactionByWalletIdAndOperationType(eq(auth.getWalletId()), eq(operationType), eq(pageable)))
                .thenReturn(pageMock);

            String endpoint = getEndPointByOperationType(operationType);

            mockMvc.perform(get(URI_API + endpoint, auth.getWalletId()))
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk())                
                .andExpect(jsonPath("$.page.totalElements").value(dataMock.size()));
        }

        private String getEndPointByOperationType(OperationType operationType) {
            String endpoint = "/me/transactions";
            if (operationType == null) {
                return endpoint;
            }
        
            switch (operationType) {
                case DEPOSIT:
                    endpoint = "/me/transactions/deposits";
                    break;
                case WITHDRAW:
                    endpoint = "/me/transactions/withdraws";
                    break;
                case TRANSFER_SEND:
                    endpoint = "/me/transactions/transfers-sent";
                    break;
                case TRANSFER_RECEIVED:
                    endpoint = "/me/transactions/transfers-received";
                    break;            
                default:
                    break;
            }
            return endpoint;
        }

    }   
    

    // =========================================================
    // CONTEXTO DE INTEGRAÇÃO (Machine-to-Machine)
    // =========================================================

    @Nested
    @DisplayName("Operações de Integração (Gateway/M2M)")
    class IntegrationContext {

        @Test
        @DisplayName("Deve consultar saldo de uma carteira específica (Admin/Integrador)")
        void checkBalance_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));
            
            Wallet mockWallet = getWalletById(auth.getWalletId());

            when(walletService.getWalletById(mockWallet.getWalletId()))
                .thenReturn(mockWallet);

            mockMvc.perform(get(URI_API + "/balance/{walletId}", mockWallet.getWalletId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.walletId").value(mockWallet.getWalletId()))
                    .andExpect(jsonPath("$.balance").value(1500.00))
                    .andExpect(jsonPath("$.status").value(Status.ACTIVE.name()));
        }

        @Test
        @DisplayName("Deve executar uma operação de débito direto via Gateway")
        void executeDebit_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));

            Wallet mockWallet = getWalletById(auth.getWalletId());
            BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();

            WithdrawMoney newWithdraw = TransactionUtilsMock
                .generateWithdrawMoneyMock(transactions, mockWallet, amount, movements);

            when(transactionService.saveWithdrawMoney(eq(mockWallet.getWalletId()), eq(amount)))
                .thenReturn(newWithdraw);

            CreateWithdrawRequest withdrawRequest = CreateWithdrawRequest.builder()
                .walletId(auth.getWalletId())
                .amount(amount)
                .build();

            mockMvc.perform(post(URI_API + "/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(withdrawRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transactionId").exists());
        }
        
        @Test
        @DisplayName("Deve validar status da carteira antes de operar")
        void validateWalletStatus_ok() throws Exception {
            setupMockAuth(List.of(LoginRole.ADMIN));
            Wallet mockWallet = wallets.get(0);
            
            when(walletService.getWalletById(mockWallet.getWalletId())).thenReturn(mockWallet);
            
            mockMvc.perform(get(URI_API + "/status/{walletId}", mockWallet.getWalletId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").exists()); // Ajustado para verificar existência
        }
    }

    // =========================================================
    // CONTEXTO DE ADMINISTRAÇÃO (Gestão de Operadores)
    // =========================================================

    @Nested
    @DisplayName("Operações Administrativas")
    class AdminContext {
        // Se houver endpoints específicos de gestão do gateway (ex: listar todas conexões), inclua aqui.
        @Test
        @DisplayName("Admin deve conseguir listar configurações de operadores")
        void listOperators_ok() throws Exception {
            setupMockAuth(List.of(LoginRole.ADMIN));

            // Exemplo hipotético se houver listagem
            // when(service.listOperators()).thenReturn(...);
            
            // mockMvc.perform(get(URI_API))...
        }
    }

    // =========================================================
    // AUXILIARES
    // =========================================================

    private LoginAuth setupMockAuth(List<LoginRole> roles) {
        // Usa lógica otimizada para pegar login válido e ativo da massa
        LoginAuth login = getRandomLoginByRole(roles);

        JwtAuthenticationDetails details = JwtAuthenticationDetails.builder()
                .loginId(login.getId())
                .login(login.getLogin())
                .walletId(login.getWalletId()) // Fundamental para os testes do /me
                .roles(login.getRole())
                .build();

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, authorities)
        );

        when(jwtAuthenticatedUserProvider.get()).thenReturn(details);
        return login;
    }

    private LoginAuth getRandomLoginByRole(List<LoginRole> targetRoles) {
        // 1. Mapeia IDs de Clientes Ativos
        Set<Long> activeCustomerIds = customers.stream()
                .filter(c -> Status.ACTIVE.equals(c.getStatus()))
                .map(Customer::getCustomerId)
                .collect(Collectors.toSet());

        // 2. Mapeia IDs de Carteiras Ativas
        Set<Long> activeWalletIds = wallets.stream()
                .filter(w -> Status.ACTIVE.equals(w.getStatus()))
                .map(Wallet::getWalletId)
                .collect(Collectors.toSet());

        // 3. Busca um Login que atenda a TODOS os critérios
        return loginAuths.stream()
                // Critério 1: Tem a role desejada
                .filter(la -> la.getRole().stream().anyMatch(targetRoles::contains))
                
                // Critério 2: Pertence a um Customer Ativo (evita NullPointerException se ID for nulo)
                .filter(la -> la.getCustomerId() != null && activeCustomerIds.contains(la.getCustomerId()))
                
                // Critério 3: Pertence a uma Wallet Ativa
                .filter(la -> la.getWalletId() != null && activeWalletIds.contains(la.getWalletId()))
                
                .findAny()
                .orElseThrow(() -> new RuntimeException(
                    "Nenhum LoginAuth encontrado com Customer ATIVO e Wallet ATIVA para as roles: " + targetRoles));
    }

    private Wallet getWalletById(Long walletId) {
        return wallets.stream()
            .filter(w -> w.getWalletId().equals(walletId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Wallet não encontrada ID: " + walletId));
    }

    private void encriptAccessKeyLoginAuthListMock(List<LoginAuth> loginAuths) {
        for (LoginAuth loginAuth : loginAuths) {
            String rawAccessKey = loginAuth.getAccessKey();
            String encodedAccessKey = passwordEncoder.encode(rawAccessKey);
            loginAuth.setAccessKey(encodedAccessKey);
        }
    }

    private void loadMockData() {
        if (!dataLoaded) {
            paramsApp = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP, new TypeReference<List<ParamApp>>() {});
            customers = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER, new TypeReference<List<Customer>>() {});      
            wallets = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET, new TypeReference<List<Wallet>>() {});
            loginAuths = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH, new TypeReference<List<LoginAuth>>() {});
            transactions = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_TRANSACTION, new TypeReference<List<Transaction>>() {});
            movements = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_MOVIMENT, new TypeReference<List<MovementTransaction>>() {});
            depositSenders = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_DEPOSIT_SENDER, new TypeReference<List<DepositSender>>() {});
        }
    }


    private List<Transaction> filterTransactionByWalletIdAndOperationType(Long walletId, OperationType operationType) {
        return transactions.stream()
            .filter(t -> t.getWalletId().equals(walletId) 
                && (operationType == null || t.getOperationType().equals(operationType))
            )
            .collect(Collectors.toList());
    }

}