package com.guga.walletserviceapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.RandomMock;
import com.guga.walletserviceapi.helpers.TransactionUtils;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.model.*;
import com.guga.walletserviceapi.model.enums.CompareBigDecimal;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.model.enums.StatusTransaction;
import com.guga.walletserviceapi.repository.CustomerRepository;
import com.guga.walletserviceapi.repository.TransactionRepository;
import com.guga.walletserviceapi.repository.WalletRepository;
import com.guga.walletserviceapi.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "user", roles = {"USER"})
@ActiveProfiles("test")
@SpringBootTest
class TransactionControllerTests {

    private static boolean SAVE_JSON = true;
    private static boolean LOAD_JSON = false;

    private static String JSON_FILE_CUSTOMER = "json_customers.json";
    private static String JSON_FILE_WALLET = "json_wallets.json";
    private static String JSON_FILE_TRANSACTION = "json_transactions.json";
    private static String JSON_FILE_MOVIMENT = "json_movements.json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private WalletRepository walletRepository;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @Value("${controller.path.base}")
    private String BASE_PATH;

    private static final String API_NAME = "/transactions";

    private String URI_API;

    private List<Customer> customers;

    private List<Wallet> wallets;

    private List<Transaction> transactions;

    private List<MovementTransaction> movementTransactionList;

    private static long sequenceTransaction;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        Mockito.reset(transactionService);
        URI_API = BASE_PATH.concat(API_NAME);

        if (!objectMapper.getRegisteredModuleIds().contains("jackson-datatype-jsr310")) {
            objectMapper.registerModule(new JavaTimeModule());
        }

        simularTransacoes();

    }

    private void simularTransacoes() {
        boolean isOlderThanFiveMinutes = FileUtils.isOlderThanFiveMinutes(JSON_FILE_CUSTOMER);

        try{
            if (LOAD_JSON && LOAD_JSON && !isOlderThanFiveMinutes) {
                TypeReference<List<Customer>> customerTypeRef = new TypeReference<List<Customer>>() {};
                customers = FileUtils.loadListFromFile(JSON_FILE_CUSTOMER, customerTypeRef);

                TypeReference<List<Wallet>> walletTypeRef = new TypeReference<List<Wallet>>() {};
                wallets = FileUtils.loadListFromFile(JSON_FILE_WALLET, walletTypeRef);

                TypeReference<List<Transaction>> transactionTypeRef = new TypeReference<List<Transaction>>() {};
                transactions = FileUtils.loadListFromFile(JSON_FILE_TRANSACTION, transactionTypeRef);

                TypeReference<List<MovementTransaction>> movementTypeRef = new TypeReference<List<MovementTransaction>>() {};
                movementTransactionList = FileUtils.loadListFromFile(JSON_FILE_MOVIMENT, movementTypeRef);
            }
            else {
                customers = TransactionUtilsMock.createCustomerListMock();
                TransactionUtilsMock.getCustomersByStatus(customers, Status.ACTIVE);
                //customerRepository.saveAll(customers);

                wallets = TransactionUtilsMock.createWalletListMock( customers );
                TransactionUtilsMock.getWalletsByStatus(wallets, Status.ACTIVE );
                //walletRepository.saveAll(wallets);

                List<MovementTransaction> movementTransactionList = new ArrayList<>();

                transactions = TransactionUtilsMock.createTransactionListMock(wallets, movementTransactionList);
                //transactionRepository.saveAll(transactions);

                if(SAVE_JSON) {
                    if (!objectMapper.getRegisteredModuleIds().contains("jackson-datatype-jsr310")) {
                        objectMapper.registerModule(new JavaTimeModule());
                    }


                    FileUtils.writeStringToFile(JSON_FILE_CUSTOMER, objectMapper.writeValueAsString(customers) );
                    FileUtils.writeStringToFile(JSON_FILE_WALLET, objectMapper.writeValueAsString(wallets) );
                    FileUtils.writeStringToFile(JSON_FILE_TRANSACTION, objectMapper.writeValueAsString(transactions) );
                    FileUtils.writeStringToFile(JSON_FILE_MOVIMENT, objectMapper.writeValueAsString(movementTransactionList) );
                }
            }

        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }

    }

    @DisplayName("shouldReturn200_WhenRequestTransactionById >> Solicita uma transação a partir de um transactionId válido")
    @Test
    void shouldReturn200_WhenRequestTransactionById() throws Exception {
        int idxTransaction = RandomMock.generateIntNumberByInterval(0,  transactions.size() - 1);
        Transaction transaction = transactions.get(idxTransaction);
        Long transactionId = transaction.getTransactionId();

        when(transactionService.getTransactionById(any(Long.class))).thenReturn(transaction);

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/{id}"), transactionId)
                        .param("id", transactionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString())) // <<<<<<< VERIFIQUE ISTO
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is(transaction.getTransactionId().intValue())))
                .andExpect(jsonPath("$.walletId", is(transaction.getWalletId().intValue())))
                ;

    }

    @Test
    @DisplayName("shouldReturn200_WhenRequestTListTransactionByWalletId >> Solicita uma transação a partir de um transactionId válido")
    void shouldReturn200_WhenRequestTListTransactionByWalletId() throws Exception {

        int idxTransaction = RandomMock.generateIntNumberByInterval(0,  transactions.size() - 1);
        Transaction transaction = transactions.get(idxTransaction);
        Long walletId = transaction.getWalletId();

        List<Transaction> resultFilter = transactions.stream()
                .filter(trn -> trn.getWalletId().compareTo(walletId) == CompareBigDecimal.EQUAL.getValue() )
                .toList();

        Page<Transaction> pageForWallet = new PageImpl<>(resultFilter, TransactionUtilsMock.getDefaultPageable(), resultFilter.size());

        when(transactionService.getTransactionByWalletId(any(Long.class), any(Pageable.class)))
                .thenReturn(pageForWallet);

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/search"))
                        .param("walletId", String.valueOf(walletId))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(resultFilter.size())))
                .andExpect(jsonPath("$.totalElements").value(resultFilter.size()));
    }

    @Test
    @DisplayName("shouldReturn200_WhenRequestTListTransactionByWalletIdAndProcessType >> Solicita as transações por wallet e status")
    void shouldReturn200_WhenRequestTListTransactionByWalletIdAndProcessType() throws Exception {

        int idxTransaction = RandomMock.generateIntNumberByInterval(0,  transactions.size() - 1);
        Transaction transaction = transactions.get(idxTransaction);
        Long walletId = transaction.getWalletId();
        StatusTransaction typeTransaction = transaction.getStatusTransaction();

        List<Transaction> resultFilter = transactions.stream()
                .filter(trn -> trn.getWalletId().compareTo(walletId) == CompareBigDecimal.EQUAL.getValue()
                            && trn.getStatusTransaction().equals(typeTransaction))
                .toList();

        Page<Transaction> pageForWallet = new PageImpl<>(resultFilter, TransactionUtilsMock.getDefaultPageable(), resultFilter.size());

        when(transactionService.filterTransactionByWalletIdAndProcessType(any(Long.class),
                any(StatusTransaction.class), any(Pageable.class))
        ).thenReturn(pageForWallet);

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/list"))
                        .param("walletId", String.valueOf(walletId))
                        .param("typeTransaction", typeTransaction.name())
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(resultFilter.size())))
                .andExpect(jsonPath("$.totalElements").value(resultFilter.size()));
    }

    @Test
    @DisplayName("shouldReturn201_WhenCreateNewDepositMoney >> Solicita as transações por wallet e status")
    void shouldReturn201_WhenCreateNewDepositMoney() throws Exception {
        int idxWalletSend = RandomMock.generateIntNumberByInterval(0,  wallets.size() - 1);
        Wallet wallet = wallets.get(idxWalletSend);

        BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();

        DepositMoney depositMoney = TransactionUtils.generateDepositMoney(wallet, amount);

        when(transactionService.saveDepositMoney( any(Long.class), any(BigDecimal.class),
                any(String.class), any(String.class), any(String.class) )
            )
            .thenReturn(depositMoney);

        Long newTransactionId = lastTransactionId() + 1;
        depositMoney.setTransactionId(newTransactionId);

        String cpfSender = TransactionUtilsMock.cpfFake();
        String senderName = TransactionUtilsMock.fullNameFake();
        String terminalId = RandomMock.generateHexaBetween100And999()
                .concat(RandomMock.generateRandomNumbers(5));

        // ACT & ASSERT: Execução do MockMvc
        mockMvc.perform(post(URI_API.concat("/transaction"))
                        .param("type", "DEPOSIT")
                        .param("walletId", String.valueOf( depositMoney.getWalletId()) )
                        .param("amount", String.valueOf(amount) )
                        .param("cpfSender", cpfSender )
                        .param("terminalId", terminalId )
                        .param("senderName", senderName )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositMoney))
                )
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.transactionId").value(depositMoney.getTransactionId()))
                .andExpect(jsonPath("$.walletId").value(depositMoney.getWalletId()));

    }

    @Test
    @DisplayName("shouldReturn201_WhenCreateNewWithdrawMoney >> Solicita as transações por wallet e status")
    void shouldReturn201_WhenCreateNewWithdrawMoney() throws Exception {
        int idxWalletSend = RandomMock.generateIntNumberByInterval(0,  wallets.size() - 1);
        Wallet wallet = wallets.get(idxWalletSend);

        BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();

        WithdrawMoney withdrawMoney = TransactionUtils.generateWithdraw(wallet, amount);
        when(transactionService.saveWithdrawMoney( any(Long.class), any(BigDecimal.class) ))
            .thenReturn(withdrawMoney);

        Long newTransactionId = lastTransactionId() + 1;
        withdrawMoney.setTransactionId(newTransactionId);

        // ACT & ASSERT: Execução do MockMvc
        mockMvc.perform(post(URI_API.concat("/transaction"))
                        .param("type", "WITHDRAW")
                        .param("walletId", String.valueOf(wallet.getWalletId()))
                        .param("amount", String.valueOf(amount))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawMoney))
                )
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.transactionId").value(withdrawMoney.getTransactionId()))
                .andExpect(jsonPath("$.walletId").value(withdrawMoney.getWalletId()));

    }

    @Test
    @DisplayName("shouldReturn201_WhenCreateNewTransferMoneySend >> Solicita as transações por wallet e status")
    void shouldReturn201_WhenCreateNewTransferMoneySend() throws Exception {
        int idxWalletSend = RandomMock.generateIntNumberByInterval(0,  wallets.size() - 1);
        Wallet walletSend = wallets.get(idxWalletSend);

        int idxWalletReceived = RandomMock.generateIntNumberByInterval(0,  wallets.size() - 1);
        Wallet walletReceived = wallets.get(idxWalletReceived);

        BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();

        TransferMoneySend moneySend = TransactionUtils.generateTransferMoneySend(
                walletSend, walletReceived, amount);

        when(transactionService.saveTransferMoneySend( any(Long.class), any(Long.class), any(BigDecimal.class) )
            )
            .thenReturn(moneySend);

        Long newTransactionId = lastTransactionId() + 1;
        moneySend.setTransactionId(newTransactionId);

        // ACT & ASSERT: Execução do MockMvc
        mockMvc.perform(post(URI_API.concat("/transaction"))
                        .param("type", "TRANSFER_SEND")
                        .param("walletIdSend", String.valueOf(walletSend.getWalletId()) )
                        .param("walletIdReceived", String.valueOf(walletReceived.getWalletId()) )
                        .param("amount", String.valueOf(amount))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moneySend))
                )
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                //.andExpect(jsonPath("$.transactionType").value("TRANSFER_SEND"))
                .andExpect(jsonPath("$.transactionId").value(moneySend.getTransactionId()))
                .andExpect(jsonPath("$.walletId").value(moneySend.getWalletId()));

    }

    private Long lastTransactionId() {
        return transactions.stream()
                .max(Comparator.comparing(Transaction::getTransactionId))
                .map(Transaction::getTransactionId)
                .orElse(0L);
    }

}
