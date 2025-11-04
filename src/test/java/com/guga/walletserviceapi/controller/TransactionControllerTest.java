package com.guga.walletserviceapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.Helper;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.service.TransactionService;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@WebMvcTest(TransactionController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "user", roles = {"USER"})
public class TransactionControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @Value("${controller.path.base}")
    private String BASE_PATH;

    private static final String API_NAME = "/transactions";

    private String URI_API;

    private Faker faker;

    private List<Customer> customers;

    private List<Wallet> wallets;

    private List<Transaction> transactions;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        customers = Helper.getCustomersByStatus(
            Helper.createCustomerListMock(),
            Status.ACTIVE);
        if (!customers.isEmpty()) {
            wallets = Helper.getWalletsByStatus(
                Helper.createWalletListMock( customers ),
                Status.ACTIVE);
            if (!wallets.isEmpty()) {
                transactions = Helper.createTransactionListMock(wallets);
            }
        }

        FileUtils.writeStringToFile("jsonTransactionControllerTest.json",
                objectMapper.writeValueAsString(transactions) );
    }

    @Test
    void createTransaction() {
        System.out.println("sdfsdfd");
    }
//
//    @Test
//    void getAllTransactions() {
//    }
//
//    @Test
//    void getTransactionById() {
//    }
//
//    @Test
//    void findByWalletWalletIdAndCreatedAtBetween() {
//    }
}
