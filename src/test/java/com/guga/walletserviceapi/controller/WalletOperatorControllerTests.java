package com.guga.walletserviceapi.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.guga.walletserviceapi.dto.transaction.TransactionMapper;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.GlobalHelper;
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
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.service.CustomerService;
import com.guga.walletserviceapi.service.DepositSenderService;
import com.guga.walletserviceapi.service.MovementTransactionService;
import com.guga.walletserviceapi.service.TransactionService;
import com.guga.walletserviceapi.service.WalletService;

@WebMvcTest(WalletOperatorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TransactionMapper.class})
class WalletOperatorControllerTests extends BaseControllerTest {
    
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

            ResultActions ra = performRequest(HttpMethod.GET, Routers.WALLET_OPERATOR + endpoint, null, null);
            LOGGER.info(LogMarkers.LOG, ra);
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

    private Wallet getWalletById(Long walletId) {
        return wallets.stream()
            .filter(w -> w.getWalletId().equals(walletId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Wallet não encontrada ID: " + walletId));
    }

    @Override
    public void loadMockData() {
        paramsApp = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP, new TypeReference<List<ParamApp>>() {});
        customers = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER, new TypeReference<List<Customer>>() {});      
        wallets = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET, new TypeReference<List<Wallet>>() {});
        loginAuths = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH, new TypeReference<List<LoginAuth>>() {});
        transactions = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_TRANSACTION, new TypeReference<List<Transaction>>() {});
        movements = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_MOVIMENT, new TypeReference<List<MovementTransaction>>() {});
        depositSenders = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_DEPOSIT_SENDER, new TypeReference<List<DepositSender>>() {});
    }


    private List<Transaction> filterTransactionByWalletIdAndOperationType(Long walletId, OperationType operationType) {
        return transactions.stream()
            .filter(t -> t.getWalletId().equals(walletId) 
                && (operationType == null || t.getOperationType().equals(operationType))
            )
            .collect(Collectors.toList());
    }

}