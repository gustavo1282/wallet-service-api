package com.guga.walletserviceapi.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.RandomMock;
import com.guga.walletserviceapi.helpers.TransactionUtils;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.TransferMoneySend;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.WithdrawMoney;
import com.guga.walletserviceapi.model.enums.CompareBigDecimal;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.model.enums.StatusTransaction;
import com.guga.walletserviceapi.repository.CustomerRepository;
import com.guga.walletserviceapi.repository.TransactionRepository;
import com.guga.walletserviceapi.repository.WalletRepository;
import com.guga.walletserviceapi.service.TransactionService;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(username = "user", roles = {"USER"})
class TransactionControllerTests {

    private static boolean SAVE_JSON = true;
    private static boolean LOAD_JSON = true;

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

    @Autowired
    private Environment env;

    private static final String API_NAME = "/transactions";

    private String URI_API;

    private List<Customer> customers;

    private List<Wallet> wallets;

    private List<Transaction> transactions;

    private List<MovementTransaction> movements;

    private List<DepositSender> depositSenders;

    private static long sequenceTransaction;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        Mockito.reset(transactionService);

        objectMapper = FileUtils.instanceObjectMapper();

        URI_API = //env.getProperty("server.protocol-type") +
                  //      "://" +
                  //      env.getProperty("server.hostname") +
                  //      ":" +
                  //      env.getProperty("server.port") +
                  //      env.getProperty("server.servlet.context-path") +
                        env.getProperty("controller.path.base")  +
                        API_NAME
                    ;        

        simularTransacoes();
    }

    private void simularTransacoes() {

        boolean isOlderThanFiveMinutes = FileUtils.isOlderThanFiveMinutes(
                createFileJson(FileUtils.JSON_FILE_CUSTOMER));

        try {
            if (SAVE_JSON && LOAD_JSON && !isOlderThanFiveMinutes) {

                customers = FileUtils.loadJSONToListObject(createFileJson(FileUtils.JSON_FILE_CUSTOMER),
                        Customer.class);

                wallets = FileUtils.loadJSONToListObject(createFileJson(FileUtils.JSON_FILE_WALLET),
                        Wallet.class);

                depositSenders = FileUtils.loadJSONToListObject(
                        createFileJson(FileUtils.JSON_FILE_DEPOSIT_SENDER),
                        DepositSender.class);

                transactions = FileUtils.loadJSONToListObject(
                        createFileJson(FileUtils.JSON_FILE_TRANSACTION), Transaction.class);

                movements = FileUtils.loadJSONToListObject(createFileJson(FileUtils.JSON_FILE_MOVIMENT),
                        MovementTransaction.class);

            } else {
                customers = TransactionUtilsMock.createCustomerMock();
                TransactionUtilsMock.findCustomersByStatus(customers, Status.ACTIVE);

                wallets = TransactionUtilsMock.createWalletMock(customers);
                TransactionUtilsMock.findWalletsByStatus(wallets, Status.ACTIVE);

                Map<String, List<?>> resultMapTransactions = TransactionUtilsMock
                        .createTransactionMock(wallets);

                if (SAVE_JSON) {

                    objectMapper = FileUtils.instanceObjectMapper();

                    FileUtils.writeStringToFile(createFileJson(FileUtils.JSON_FILE_CUSTOMER),
                            objectMapper.writeValueAsString(customers));

                    FileUtils.writeStringToFile(createFileJson(FileUtils.JSON_FILE_WALLET),
                            objectMapper.writeValueAsString(wallets));

                    transactions = (List<Transaction>) resultMapTransactions.get("transactions");
                    movements = (List<MovementTransaction>) resultMapTransactions.get("movements");
                    depositSenders = (List<DepositSender>) resultMapTransactions
                            .get("depositSenders");

                    FileUtils.writeStringToFile(createFileJson(FileUtils.JSON_FILE_TRANSACTION),
                            objectMapper.writeValueAsString(transactions));

                    FileUtils.writeStringToFile(createFileJson(FileUtils.JSON_FILE_MOVIMENT),
                            objectMapper.writeValueAsString(movements));

                    FileUtils.writeStringToFile(createFileJson(FileUtils.JSON_FILE_DEPOSIT_SENDER),
                            objectMapper.writeValueAsString(depositSenders));

                }
            }

        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }

    }

    private String createFileJson(String jsonFile) {

        return FileUtils.FOLDER_DEFAULT_FILE_JSON +
                jsonFile;

    }

    @DisplayName("shouldReturn200_WhenRequestTransactionById >> Solicita uma transação a partir de um transactionId válido")
    @Test
    void shouldReturn200_WhenRequestTransactionById() throws Exception {
        int idxTransaction = RandomMock.generateIntNumberByInterval(0, transactions.size() - 1);
        Transaction transaction = transactions.get(idxTransaction);
        Long transactionId = transaction.getTransactionId();

        when(transactionService.getTransactionById(any(Long.class))).thenReturn(transaction);

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/{id}"), transactionId)
                .param("id", transactionId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out
                        .println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is(transaction.getTransactionId().intValue())))
                .andExpect(jsonPath("$.walletId",
                        is(transaction.getWalletId().intValue())));
    }

    @Test
    @DisplayName("shouldReturn200_WhenRequestTListTransactionByWalletId >> Solicita uma transação a partir de um transactionId válido")
    void shouldReturn200_WhenRequestTListTransactionByWalletId() throws Exception {

        int idxTransaction = RandomMock.generateIntNumberByInterval(0, transactions.size() - 1);
        Transaction transaction = transactions.get(idxTransaction);
        Long walletId = transaction.getWalletId();

        List<Transaction> resultFilter = transactions.stream()
                .filter(trn -> trn.getWalletId()
                        .compareTo(walletId) == CompareBigDecimal.EQUAL.getValue())
                .toList();

        Page<Transaction> pageForWallet = new PageImpl<>(resultFilter,
                TransactionUtilsMock.getDefaultPageable(),
                resultFilter.size());

        when(transactionService.getTransactionByWalletId(any(Long.class), any(Pageable.class)))
                .thenReturn(pageForWallet);

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/search-wallet"))
                .param("walletId", String.valueOf(walletId))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out
                        .println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(resultFilter.size())))
                .andExpect(jsonPath("$.page.totalElements").value(resultFilter.size()));
    }

    @Test
    @DisplayName("shouldReturn200_WhenRequestTListTransactionByWalletIdAndProcessType >> Solicita as transações por wallet e status")
    void shouldReturn200_WhenRequestTListTransactionByWalletIdAndProcessType() throws Exception {

        int idxTransaction = RandomMock.generateIntNumberByInterval(0, transactions.size() - 1);
        Transaction transaction = transactions.get(idxTransaction);
        Long walletId = transaction.getWalletId();
        StatusTransaction typeTransaction = transaction.getStatusTransaction();

        List<Transaction> resultFilter = transactions.stream()
                .filter(trn -> trn.getWalletId()
                        .compareTo(walletId) == CompareBigDecimal.EQUAL
                                .getValue()
                        && trn.getStatusTransaction().equals(typeTransaction))
                .toList();

        Page<Transaction> pageForWallet = new PageImpl<>(resultFilter,
                TransactionUtilsMock.getDefaultPageable(),
                resultFilter.size());

        when(transactionService.filterTransactionByWalletIdAndProcessType(any(Long.class),
                any(StatusTransaction.class), any(Pageable.class))).thenReturn(pageForWallet);

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/list"))
                .param("walletId", String.valueOf(walletId))
                .param("typeTransaction", typeTransaction.name())
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out
                        .println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(resultFilter.size())))
                .andExpect(jsonPath("$.page.totalElements").value(resultFilter.size()));
    }

    @Test
    @DisplayName("shouldReturn201_WhenCreateNewDepositMoney >> Solicita as transações por wallet e status")
    void shouldReturn201_WhenCreateNewDepositMoney() throws Exception {
        int idxWalletSend = RandomMock.generateIntNumberByInterval(0, wallets.size() - 1);
        Wallet wallet = wallets.get(idxWalletSend);

        BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();

        DepositMoney depositMoney = TransactionUtils.generateDepositMoney(wallet, amount);

        when(transactionService.saveDepositMoney(any(Long.class), any(BigDecimal.class),
                any(String.class), any(String.class), any(String.class)))
                .thenReturn(depositMoney);

        Long newTransactionId = lastTransactionId() + 1;
        depositMoney.setTransactionId(newTransactionId);

        String cpfSender = RandomMock.cpfFake();
        String senderName = RandomMock.loginFakeMock();
        String terminalId = RandomMock.generateHexaBetween100And999()
                .concat(RandomMock.generateRandomNumbers(5));

        // ACT & ASSERT: Execução do MockMvc
        mockMvc.perform(post(URI_API.concat("/transaction"))
                .param("type", "DEPOSIT")
                .param("walletId", String.valueOf(depositMoney.getWalletId()))
                .param("amount", String.valueOf(amount))
                .param("cpfSender", cpfSender)
                .param("terminalId", terminalId)
                .param("senderName", senderName)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositMoney)))
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out
                        .println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.transactionId").value(depositMoney.getTransactionId()))
                .andExpect(jsonPath("$.walletId").value(depositMoney.getWalletId()));

    }

    @Test
    @DisplayName("shouldReturn201_WhenCreateNewWithdrawMoney >> Solicita as transações por wallet e status")
    void shouldReturn201_WhenCreateNewWithdrawMoney() throws Exception {
        int idxWalletSend = RandomMock.generateIntNumberByInterval(0, wallets.size() - 1);
        Wallet wallet = wallets.get(idxWalletSend);

        BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();

        WithdrawMoney withdrawMoney = TransactionUtils.generateWithdraw(wallet, amount);
        when(transactionService.saveWithdrawMoney(any(Long.class), any(BigDecimal.class)))
                .thenReturn(withdrawMoney);

        Long newTransactionId = lastTransactionId() + 1;
        withdrawMoney.setTransactionId(newTransactionId);

        // ACT & ASSERT: Execução do MockMvc
        mockMvc.perform(post(URI_API.concat("/transaction"))
                .param("type", "WITHDRAW")
                .param("walletId", String.valueOf(wallet.getWalletId()))
                .param("amount", String.valueOf(amount))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawMoney)))
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out
                        .println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.transactionId").value(withdrawMoney.getTransactionId()))
                .andExpect(jsonPath("$.walletId")
                        .value(withdrawMoney.getWalletId()));

    }

    @Test
    @DisplayName("shouldReturn201_WhenCreateNewTransferMoneySend >> Solicita as transações por wallet e status")
    void shouldReturn201_WhenCreateNewTransferMoneySend() throws Exception {
        int idxWalletSend = RandomMock.generateIntNumberByInterval(0, wallets.size() - 1);
        Wallet walletSend = wallets.get(idxWalletSend);

        int idxWalletReceived = RandomMock.generateIntNumberByInterval(0, wallets.size() - 1);
        Wallet walletReceived = wallets.get(idxWalletReceived);

        BigDecimal amount = TransactionUtilsMock.generateMoneyBetweenMinAndMaxValue();

        TransferMoneySend moneySend = TransactionUtils.generateTransferMoneySend(
                walletSend, walletReceived, amount);

        when(transactionService.saveTransferMoneySend(any(Long.class), any(Long.class), any(BigDecimal.class)))
                .thenReturn(moneySend);

        Long newTransactionId = lastTransactionId() + 1;
        moneySend.setTransactionId(newTransactionId);

        // ACT & ASSERT: Execução do MockMvc
        mockMvc.perform(post(URI_API.concat("/transaction"))
                .param("type", "TRANSFER_SEND")
                .param("walletIdSend", String.valueOf(walletSend.getWalletId()))
                .param("walletIdReceived", String.valueOf(walletReceived.getWalletId()))
                .param("amount", String.valueOf(amount))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(moneySend)))
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out
                        .println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                // .andExpect(jsonPath("$.transactionType").value("TRANSFER_SEND"))
                .andExpect(jsonPath("$.transactionId").value(moneySend.getTransactionId()))
                .andExpect(jsonPath("$.walletId").value(moneySend.getWalletId()));

    }

    private Long lastTransactionId() {
        return transactions.stream()
                .max(Comparator.comparing(Transaction::getTransactionId))
                .map(Transaction::getTransactionId)
                .orElse(0L);
    }

    @Test
    @DisplayName("shouldReturn200_WhenUploadFileCsvAndWriteByJSONInDatabase >> Simula upload do transaction.csv")
    void shouldReturn200_WhenUploadFileCsvAndWriteByJSONInDatabase() throws Exception {

        String VALID_TRANSACTION_CSV = "\"transactionId\",\"createdAt\",\"walletId\",\"operationType\",\"previousBalance\",\"amount\",\"currentBalance\",\"statusTransaction\",\"senderId\"\r\n"
                + //
                "11001,\"2025-11-16 02:57:18.381653500\",2119,\"DEPOSIT\",0,246.16,246.16,\"CUSTOMER_INVALID\",12901\r\n"
                + //
                "11002,\"2025-11-16 02:57:18.386653300\",2049,\"WITHDRAW\",0,365.5,-365.5,\"CUSTOMER_INVALID\",\\N\r\n"
                + //
                "11003,\"2025-11-16 02:57:18.386653300\",2058,\"WITHDRAW\",0,596.52,-596.52,\"CUSTOMER_INVALID\",\\N\r\n"
                + //
                "11004,\"2025-11-16 02:57:18.386653300\",2055,\"DEPOSIT\",0,549.23,549.23,\"CUSTOMER_INVALID\",12904\r\n"
                + //
                "11005,\"2025-11-16 02:57:18.390655100\",2062,\"DEPOSIT\",0,287.77,287.77,\"CUSTOMER_INVALID\",12905\r\n"
                + //
                "11006,\"2025-11-16 02:57:18.396656500\",2062,\"DEPOSIT\",0,488.68,488.68,\"CUSTOMER_INVALID\",\\N\r\n"
                + //
                "11007,\"2025-11-16 02:57:18.396656500\",2038,\"TRANSFER_SEND\",0,599.42,-599.42,\"CUSTOMER_INVALID\",\\N\r\n"
                + //
                "11008,\"2025-11-16 02:57:18.396656500\",2106,\"DEPOSIT\",0,545.88,545.88,\"CUSTOMER_INVALID\",12908\r\n"
                + //
                "11009,\"2025-11-16 02:57:18.399658200\",2051,\"DEPOSIT\",0,607.9,607.9,\"CUSTOMER_INVALID\",12909\r\n";

        // 1. Definições do Arquivo
        String name = "file"; // Nome do parâmetro que sua API espera (ex: @RequestParam("file"))
        String filename = "transactions.csv";
        String contentType = "text/csv";
        byte[] content = VALID_TRANSACTION_CSV.getBytes(StandardCharsets.UTF_8);

        // 2. Criação do MockMultipartFile
        MultipartFile multipartFile = new MockMultipartFile(
                name,
                filename,
                contentType,
                content);

        transactionService.loadCsvAndSave(multipartFile);

    }

}
