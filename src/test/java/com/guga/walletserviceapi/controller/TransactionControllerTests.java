package com.guga.walletserviceapi.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.TransactionUtils;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.TransferMoneySend;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.model.enums.StatusTransaction;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.security.filter.JwtAuthenticationFilter;
import com.guga.walletserviceapi.service.TransactionService;
import com.guga.walletserviceapi.service.common.DataPersistenceService;

@WebMvcTest(TransactionController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import({
        com.guga.walletserviceapi.config.ConfigProperties.class,
        com.guga.walletserviceapi.config.PasswordConfig.class,
        com.guga.walletserviceapi.service.common.DataPersistenceService.class
})
class TransactionControllerTests {

    private static BigDecimal AMOUNT_MIN_TO_DEPOSIT = new BigDecimal(20);
    private static BigDecimal AMOUNT_MIN_TO_TRANSFER = new BigDecimal(20);
    
    private static final String API_NAME = "/transactions";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtAuthenticatedUserProvider jwtAuthenticatedUserProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired 
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataPersistenceService dataPersistenceService;

    @Value("${controller.path.base}")
    private String BASE_PATH;

    private String URI_API;

    private List<Customer> customers;
    private List<Wallet> wallets;
    private List<Transaction> transactions;
    private List<MovementTransaction> movements; 
    private List<DepositSender> depositSenders;
    private List<ParamApp> paramsApp;
    private List<LoginAuth> loginAuths;

    private boolean dataLoaded;
    
    @BeforeAll
    void setUpAll() {
        URI_API = BASE_PATH + API_NAME;
        objectMapper = FileUtils.instanceObjectMapper();
        loadMockData();
    }        

// =========================================================
    // CONTEXTO DE USUÁRIO (Endereços /me)
    // =========================================================

    @Nested
    @DisplayName("Operações do Usuário Autenticado")
    class UserContext {

        @Test
        @DisplayName("Deve listar transações do próprio usuário com sucesso")
        void listMyTransactions_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of("USER"));

            Transaction transactionMock = getDinamicTransactionByWalletId(auth.getWalletId());                

            when(transactionService.filterTransactionByWalletIdAndProcessType(eq(auth.getWalletId()), any(), any()))
                .thenReturn(new PageImpl<>(List.of(transactionMock)));

            mockMvc.perform(get(URI_API + "/me")
                    .param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        @DisplayName("Deve detalhar uma transação específica do próprio usuário")
        void getMyTransaction_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of("USER"));

            Transaction transactionMock = getDinamicTransactionByWalletId(auth.getWalletId());

            when(transactionService.getTransactionById(transactionMock.getTransactionId()))
                .thenReturn(transactionMock);

            mockMvc.perform(get(URI_API + "/me/{id}", transactionMock.getTransactionId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transactionId").value(transactionMock.getTransactionId()));
        }

        @Test
        @DisplayName("Deve listar os últimos depósitos do usuário")
        void listMyDeposits_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of("USER"));

            Transaction transactionMock = getDinamicTransactionByWalletId(auth.getWalletId());

            when(transactionService.filterTransactionByWalletIdAndOperationType(eq(auth.getWalletId()), any(), any()))
                .thenReturn(new PageImpl<>(List.of(transactionMock)));

            mockMvc.perform(get(URI_API + "/me/deposits"))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================
    // CONTEXTO ADMINISTRATIVO
    // =========================================================

    @Nested
    @DisplayName("Operações Administrativas")
    class AdminContext {

        @Test
        @DisplayName("Admin deve obter qualquer transação pelo ID")
        void getTransactionById_admin_ok() throws Exception {
            setupMockAuth(List.of("ADMIN"));
            Transaction t = transactions.get(0);

            when(transactionService.getTransactionById(t.getTransactionId())).thenReturn(t);

            mockMvc.perform(get(URI_API + "/{id}", t.getTransactionId()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin deve listar transações por Wallet ID")
        void listByWallet_admin_ok() throws Exception {
            setupMockAuth(List.of("ADMIN"));
            Long targetWalletId = 123L;

            when(transactionService.filterTransactionByWalletIdAndProcessType(eq(targetWalletId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(transactions.get(0))));

            mockMvc.perform(get(URI_API + "/by-wallet/{walletId}", targetWalletId))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================
    // CRIAÇÃO DE TRANSAÇÕES (POST) Tipadas [Withdraw, Deposit, Transfer]
    // =========================================================

    @Nested
    @DisplayName("Criação de Transações Tipadas [Withdraw, Deposit, Transfer]")
    class TransactionCreation {

        @Test
        @DisplayName("Deve realizar um depósito com sucesso")
        void deposit_created() throws Exception {

            LoginAuth auth = setupMockAuth(List.of("USER"));

            BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();
            
            Wallet wallet = getWalletById(auth.getWalletId());

            DepositMoney newDepositMoney = TransactionUtils.generateDepositMoney(wallet, amount, AMOUNT_MIN_TO_DEPOSIT);
            newDepositMoney.setTransactionId(nextTransactionId());
            newDepositMoney.setDepositSender( getDepositSenderValid() );

            when(transactionService.saveDepositMoney(eq(auth.getWalletId()), any(), any(), any(), any()))
                .thenReturn(newDepositMoney);

            mockMvc.perform(post(URI_API + "/deposit")
                    .param("amount", newDepositMoney.getAmount().toString())
                    .param("cpfSender", newDepositMoney.getDepositSender().getCpf())
                    .param("terminalId", newDepositMoney.getDepositSender().getTerminalId())
                    .param("senderName", newDepositMoney.getDepositSender().getFullName()))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Deve realizar um saque com sucesso")
        void withdraw_ok() throws Exception {

            LoginAuth auth = setupMockAuth(List.of("USER"));

            BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();

            Wallet wallet = getWalletById(auth.getWalletId());
            
            var newWithdraw = TransactionUtils.generateWithdraw(wallet, amount);
            newWithdraw.setTransactionId(nextTransactionId());

            when(transactionService.saveWithdrawMoney(eq(auth.getWalletId()), eq(amount)))
                .thenReturn(newWithdraw);

            mockMvc.perform(post(URI_API + "/withdraw")
                    .param("amount", amount.toString()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve realizar uma transferência com sucesso")
        void transfer_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of("USER"));

            BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();
            
            Wallet origin = getWalletById(auth.getWalletId());

            Wallet destination = getWalletReceived(auth.getWalletId());

            // Analisado e Aplicado: Lógica de transferência encapsulada
            TransferMoneySend newTransferSend = TransactionUtils.generateTransferMoneySend(origin, destination, amount, AMOUNT_MIN_TO_TRANSFER);
            newTransferSend.setTransactionId(nextTransactionId());

            when(transactionService.saveTransferMoneySend(eq(origin.getWalletId()), eq(destination.getWalletId()), any()))
                .thenReturn(newTransferSend);

            mockMvc.perform(post(URI_API + "/transfer")
                    .param("walletIdReceived", destination.getWalletId().toString())
                    .param("amount", amount.toString()))
                    .andExpect(status().isOk());
        }
    }


    private void loadMockData() {
        if (!dataLoaded) {
            paramsApp = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP, new TypeReference<List<ParamApp>>() {});
            customers = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP, new TypeReference<List<Customer>>() {});      
            wallets = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET, new TypeReference<List<Wallet>>() {});
            loginAuths = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH, new TypeReference<List<LoginAuth>>() {});
            transactions = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_TRANSACTION, new TypeReference<List<Transaction>>() {});
            movements = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_MOVIMENT, new TypeReference<List<MovementTransaction>>() {});
            depositSenders = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_DEPOSIT_SENDER, new TypeReference<List<DepositSender>>() {});
        }
    }


    // =========================================================
    // MÉTODOS UTILITÁRIOS (SETUP)
    // =========================================================

    private LoginAuth setupMockAuth(List<String> targetRoles) {
        LoginAuth login = getRandomLoginByRole(targetRoles);

        JwtAuthenticationDetails details = JwtAuthenticationDetails.builder()
                .loginId(login.getId())
                .login(login.getLogin())
                .walletId(login.getWalletId())
                .roles(login.getRole())
                .build();

        List<SimpleGrantedAuthority> authorities = login.getRole().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name().toUpperCase()))
                .toList();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, authorities)
        );

        when(jwtAuthenticatedUserProvider.get()).thenReturn(details);
        return login;
    }

    private LoginAuth getRandomLoginByRole(List<String> rolesStr) {
        // 1. Converter as roles alvo para Enum
        List<LoginRole> targetEnums = rolesStr.stream()
                .map(role -> LoginRole.valueOf(role.trim().toUpperCase()))
                .toList();

        // 2. Filtrar as transações que possuem Status SUCCESS e têm um WalletId
        List<Transaction> validTransactions = transactions.stream()
                .filter(t -> t.getStatusTransaction().equals(StatusTransaction.SUCCESS))
                .toList();

        // 4. Buscar o primeiro LoginAuth que possua a Role desejada e pertença a uma dessas transações
        return validTransactions.stream()
                .map(t -> findLoginAuthByWalletId(t.getWalletId()))
                .filter(Objects::nonNull)
                .filter(la -> la.getRole().stream()
                        .anyMatch(r -> targetEnums.contains(r)))
                .findFirst()
                .orElseThrow(() -> new ResourceBadRequestException(
                    "Não foi possível encontrar um LoginAuth com as roles " + rolesStr + " associado a uma transação de sucesso."));    
    }

    // Helper auxiliar para buscar o login pelo ID da carteira na massa de dados
    private LoginAuth findLoginAuthByWalletId(long walletId) {
        return loginAuths.stream()
                .filter(la -> la.getWalletId() == walletId)
                .findFirst()
                .orElse(null);
    }

    private Transaction getDinamicTransactionByWalletId(long walletId) throws Exception {

        List<Transaction> filteredTransactions = transactions.stream()
                .filter(t -> t.getWalletId() == walletId)
                .collect(Collectors.toList());

        if (filteredTransactions.isEmpty()) {
            throw new ResourceBadRequestException("Nenhuma transação para wallet " + walletId);
        }

        Collections.shuffle(filteredTransactions);
        return filteredTransactions.get(0);

    }

    private long nextTransactionId() {
        return transactions.stream()
            .mapToLong(Transaction::getTransactionId)
            .max()
            .orElse(0L) + 1;
    }

    private DepositSender getDepositSenderValid() {
        return depositSenders.stream()
                .findAny()
                .orElse(null);
    }

    private Wallet getWalletById(Long walletId) {
        return wallets.stream()
            .filter(w -> w.getWalletId() == walletId)
            .findFirst()
            .orElseThrow(() -> new ResourceBadRequestException("Wallet não encontrada para o LoginAuth"));
    }

    private Wallet getWalletReceived(Long walletId) {
        return wallets.stream()
            .filter(w -> w.getWalletId() != walletId)
            .findAny()
            .orElseThrow(() -> new ResourceBadRequestException("Wallet não encontrada para o LoginAuth"));
    }

    private void encriptAccessKeyLoginAuthListMock(List<LoginAuth> loginAuths) {
        for (LoginAuth loginAuth : loginAuths) {
                String rawAccessKey = loginAuth.getAccessKey();
                String encodedAccessKey = passwordEncoder.encode(rawAccessKey);
                loginAuth.setAccessKey(encodedAccessKey);
        }
    }

}
