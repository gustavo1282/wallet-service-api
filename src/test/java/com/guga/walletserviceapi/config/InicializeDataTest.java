package com.guga.walletserviceapi.config;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guga.walletserviceapi.controller.BaseControllerTest;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.dto.TransactionMockResult;

@WebMvcTest(controllers = InicializeDataTest.DummyController.class)
//@AutoConfigureMockMvc     // [somente quando @SpringBootTest] injeta e cria todos os beans
@Import({
    com.guga.walletserviceapi.config.ConfigProperties.class,
    com.guga.walletserviceapi.config.PasswordConfig.class,
    com.guga.walletserviceapi.service.common.DataPersistenceService.class
    }
)
class InicializeDataTest extends BaseControllerTest {

    @RestController
    static class DummyController {
    @GetMapping("/ping")
        String ping() { return "ok"; }
    }

    @Test
	void contextLoads() {
        LOGGER.info(LogMarkers.LOG, "\n" + "=".repeat(5));
        LOGGER.info(LogMarkers.LOG, "WalletServiceApplicationTests > contextLoads()");
        LOGGER.info(LogMarkers.LOG, "\n" + "=".repeat(5));

        GlobalHelper.printAllVariables(env, LOGGER);

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

    @Override
    public void loadMockData() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadMockData'");
    }

}
