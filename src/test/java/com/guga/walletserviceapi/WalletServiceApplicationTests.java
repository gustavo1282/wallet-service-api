package com.guga.walletserviceapi;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.dto.TransactionMockResult;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.security.filter.JwtAuthenticationFilter;
import com.guga.walletserviceapi.service.common.DataPersistenceService;

@SpringBootTest
@ActiveProfiles("test")
//@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({
        com.guga.walletserviceapi.config.ConfigProperties.class,
        com.guga.walletserviceapi.config.PasswordConfig.class,
        com.guga.walletserviceapi.service.common.DataPersistenceService.class
})
class WalletServiceApplicationTests {

    private static final boolean CREATE_JSON_MOCKS = true;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticatedUserProvider jwtAuthenticatedUserProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired 
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataPersistenceService dataPersistenceService;

    private List<ParamApp> paramsApp;
    private List<Wallet> wallets;
    private List<Customer> customers;
    private List<Transaction> transactions;
    private List<DepositSender> depositSenders;
    private List<LoginAuth> loginAuths;
    private List<MovementTransaction> movements;


    @Test
	void contextLoads() {
        System.out.println("============================================");
        System.out.println("WalletServiceApplicationTests > contextLoads()");
        System.out.println("============================================");

        createDataMock();
    }

    private void createDataMock() {

        String fileref = FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER;
        boolean hasFile = FileUtils.hasFile(fileref);

        if (!hasFile && CREATE_JSON_MOCKS) {            
            paramsApp = TransactionUtilsMock.createParamsAppMock();
            customers = TransactionUtilsMock.createCustomerListMock();
            wallets = TransactionUtilsMock.createWalletListMock(customers);
            loginAuths = TransactionUtilsMock.createLoginAuthListMock(wallets );
            encriptAccessKeyLoginAuthListMock(loginAuths);

            TransactionMockResult trnMockResult = TransactionUtilsMock.createTransactionListMock(wallets);
            transactions = trnMockResult.getTransactions();
            movements = trnMockResult.getMovements();
            depositSenders = trnMockResult.getDepositSenders();

            dataPersistenceService.exportToJson(paramsApp, FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP);
            dataPersistenceService.exportToJson(customers, FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER);
            dataPersistenceService.exportToJson(wallets, FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET);
            dataPersistenceService.exportToJson(loginAuths, FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH);
            dataPersistenceService.exportToJson(transactions, FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_TRANSACTION);
            dataPersistenceService.exportToJson(movements, FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_MOVIMENT);
            dataPersistenceService.exportToJson(depositSenders, FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_DEPOSIT_SENDER);
        }
    }

    private void encriptAccessKeyLoginAuthListMock(List<LoginAuth> loginAuths) {
        for (LoginAuth loginAuth : loginAuths) {
            String rawAccessKey = loginAuth.getAccessKey();
            String encodedAccessKey = passwordEncoder.encode(rawAccessKey);
            loginAuth.setAccessKey(encodedAccessKey);
        }
    }

}
