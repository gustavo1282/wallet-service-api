package com.guga.walletserviceapi.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.guga.walletserviceapi.dto.transaction.TransactionMapper;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.helpers.FileUtils;
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
import com.guga.walletserviceapi.service.TransactionService;

@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TransactionMapper.class})
class TransactionControllerTests extends BaseControllerTest {

    @MockitoBean
    private TransactionService transactionService;

// =========================================================
    // CONTEXTO DE USUÁRIO (Endereços /me)
    // =========================================================

    @Nested
    @DisplayName("Operações do Usuário Autenticado")
    class UserContext {

        @Test
        @DisplayName("Deve listar transações do próprio usuário com sucesso")
        void listMyTransactions_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));

            Transaction transactionMock = getAnyTransactionByWalletId(auth.getWalletId());

            when(transactionService.filterTransactionByWalletIdAndProcessType(eq(auth.getWalletId()), any(), any()))
                .thenReturn(new PageImpl<>(List.of(transactionMock)));

            MvcResult result = performRequest(HttpMethod.GET, Routers.TRANSACTIONS + "/me", null, params("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andReturn();

           LOGGER.info(LogMarkers.LOG, result.getResponse().getContentAsString());
        }

        @Test
        @DisplayName("Deve detalhar uma transação específica do próprio usuário")
        void getMyTransaction_ok() throws Exception {

            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));

            Transaction transactionMock = transactionUtilsMock.getAnyValidTransactionsForLogins(
                wallets, loginAuths, transactions
            );

            transactionMock.setWalletId(auth.getWalletId());
            transactionMock.setWallet(null);

            Long transactionId = transactionMock.getTransactionId();

            when(transactionService.getTransactionById(anyLong()))
                .thenReturn(transactionMock);

            String uri = Routers.TRANSACTIONS + "/me/" + transactionId;

            performRequest(HttpMethod.GET, uri, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId));
        }

        @Test
        @DisplayName("Deve listar os últimos depósitos do usuário")
        void listMyDeposits_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));

            Transaction transactionMock = getAnyTransactionByWalletId(auth.getWalletId());

            when(transactionService.filterTransactionByWalletIdAndOperationType(eq(auth.getWalletId()), any(), any()))
                .thenReturn(new PageImpl<>(List.of(transactionMock)));

            performRequest(HttpMethod.GET, Routers.TRANSACTIONS + "/me/deposits", null, null)
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
            setupMockAuth(List.of(LoginRole.ADMIN));
            Transaction t = transactions.get(0);
            when(transactionService.getTransactionById(t.getTransactionId())).thenReturn(t);

            String url = Routers.TRANSACTIONS + "/" + t.getTransactionId();
            performRequest(HttpMethod.GET, url, null, null)
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin deve listar transações por Wallet ID")
        void listByWallet_admin_ok() throws Exception {
            LoginAuth loginAuth = setupMockAuth(List.of(LoginRole.ADMIN));

            Long walletId = loginAuth.getWalletId();

            List<Transaction> trnFilter = getTransactionByWalletId(walletId);

            if (trnFilter == null) trnFilter = List.of();

            when(transactionService.filterTransactionByWalletIdAndProcessType(anyLong(), any(), any()))
                .thenReturn(new PageImpl<>(trnFilter));

            String url = Routers.TRANSACTIONS + "/" + walletId;
            performRequest(HttpMethod.GET, url, null, null)
                .andExpect(status().isOk());
        }
    }

    // =========================================================
    // CRIAÇÃO DE TRANSAÇÕES (POST) Tipadas [Withdraw, Deposit, Transfer]
    // =========================================================

/*
    @Nested
    @DisplayName("Criação de Transações Tipadas [Withdraw, Deposit, Transfer]")
    class TransactionCreation {
        @Test
        @DisplayName("Deve realizar um depósito com sucesso")
        void deposit_created() throws Exception {

            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));

            BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();
            
            Wallet wallet = getWalletById(auth.getWalletId());

            DepositMoney newDepositMoney = TransactionUtils.generateDepositMoney(wallet, amount, AMOUNT_MIN_TO_DEPOSIT);
            newDepositMoney.setTransactionId(nextTransactionId());
            newDepositMoney.setDepositSender( getDepositSenderValid() );

            when(transactionService.saveDepositMoney(eq(auth.getWalletId()), any(), any(), any(), any()))
                .thenReturn(newDepositMoney);

            transactions.add(newDepositMoney);

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

            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));

            BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();

            Wallet wallet = getWalletById(auth.getWalletId());
            
            var newWithdraw = TransactionUtils.generateWithdraw(wallet, amount);
            newWithdraw.setTransactionId(nextTransactionId());

            when(transactionService.saveWithdrawMoney(eq(auth.getWalletId()), eq(amount)))
                .thenReturn(newWithdraw);

            transactions.add(newWithdraw);

            mockMvc.perform(post(URI_API + "/withdraw")
                    .param("amount", amount.toString()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve realizar uma transferência com sucesso")
        void transfer_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));

            BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();
            
            Wallet origin = getWalletById(auth.getWalletId());

            Wallet destination = getWalletReceived(auth.getWalletId());

            // Analisado e Aplicado: Lógica de transferência encapsulada
            TransferMoneySend newTransferSend = TransactionUtils.generateTransferMoneySend(origin, destination, amount, AMOUNT_MIN_TO_TRANSFER);
            newTransferSend.setTransactionId(nextTransactionId());

            when(transactionService.saveTransferMoneySend(eq(origin.getWalletId()), eq(destination.getWalletId()), any()))
                .thenReturn(newTransferSend);

            transactions.add(newTransferSend);

            mockMvc.perform(post(URI_API + "/transfer")
                    .param("walletIdReceived", destination.getWalletId().toString())
                    .param("amount", amount.toString()))
                    .andExpect(status().isOk());
        }
    }
 */
    // Helper auxiliar para buscar o login pelo ID da carteira na massa de dados
    private LoginAuth findLoginAuthByWalletId(long walletId) {
        return loginAuths.stream()
                .filter(la -> la.getWalletId().equals(walletId))
                .findFirst()
                .orElse(null);
    }

    private List<Transaction> getTransactionByWalletId(long walletId) throws Exception {
        List<Transaction> filteredTransactions = transactions.stream()
                .filter(t -> t.getWalletId().equals(walletId))
                .collect(Collectors.toList());

        if (filteredTransactions == null || filteredTransactions.isEmpty()) {
            throw new ResourceBadRequestException("Nenhuma transação para wallet " + walletId);
        }

        return filteredTransactions;
    }

    private Transaction getAnyTransactionByWalletId(long walletId) throws Exception {

        List<Transaction> filteredTransactions = getTransactionByWalletId(walletId);

        if (filteredTransactions == null) return null;
        
        Collections.shuffle(filteredTransactions);
        return filteredTransactions.get(ThreadLocalRandom.current().nextInt(filteredTransactions.size()));

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
            .filter(w -> w.getWalletId().equals(walletId))
            .findFirst()
            .orElseThrow(() -> new ResourceBadRequestException("Wallet não encontrada para o LoginAuth"));
    }

    private Wallet getWalletReceived(Long walletId) {
        return wallets.stream()
            .filter(w -> !w.getWalletId().equals(walletId))
            .findAny()
            .orElseThrow(() -> new ResourceBadRequestException("Wallet não encontrada para o LoginAuth"));
    }

    @Override
    public void loadMockData() {
        this.paramsApp = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP, new TypeReference<List<ParamApp>>() {});
        this.customers = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER, new TypeReference<List<Customer>>() {});      
        this.wallets = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET, new TypeReference<List<Wallet>>() {});
        this.loginAuths = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH, new TypeReference<List<LoginAuth>>() {});
        this.transactions = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_TRANSACTION, new TypeReference<List<Transaction>>() {});
        this.movements = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_MOVIMENT, new TypeReference<List<MovementTransaction>>() {});
        this.depositSenders = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_DEPOSIT_SENDER, new TypeReference<List<DepositSender>>() {});
    }

}