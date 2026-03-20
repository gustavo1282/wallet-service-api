package com.guga.walletserviceapi.config;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.dto.TransactionMockResult;
import com.guga.walletserviceapi.seeder.SeedExecutor;
import com.guga.walletserviceapi.seeder.SeedOrderConfig;
import com.guga.walletserviceapi.seeder.SeedRunner;
import com.guga.walletserviceapi.service.common.DataPersistenceService;

@JdbcTest 
@Transactional(propagation = Propagation.NOT_SUPPORTED) 
@Import({
    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class,
    com.guga.walletserviceapi.config.ConfigProperties.class,
    com.guga.walletserviceapi.config.PasswordConfig.class, // Fornece o Bean real de PasswordEncoder
    com.guga.walletserviceapi.service.common.DataPersistenceService.class
})
@TestPropertySource(properties = {
    "management.otlp.logging.enabled=false",
    "management.observations.enabled=false",
    "MANAGEMENT_OTLP_ENDPOINT=http://localhost" 
})
class InicializeDataTest {

    protected static final Logger LOGGER = LogManager.getLogger(InicializeDataTest.class);
    protected static final boolean CREATE_JSON_MOCKS = true;

    @Autowired
    protected DataPersistenceService dtPersistenceService;

    @Autowired // Agora usando a implementação REAL do seu PasswordConfig
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected Environment env;

    @Autowired
    protected ApplicationContext context;

    @Value("${app.seeder.enabled:false}") 
    protected boolean seederEnabled; 

    @Test
    void contextLoads() {
        LOGGER.info(LogMarkers.LOG, "\n" + "=".repeat(10));
        LOGGER.info(LogMarkers.LOG, "WalletServiceApplicationTests > contextLoads()");
        LOGGER.info(LogMarkers.LOG, "\n" + "=".repeat(10));

        GlobalHelper.printAllVariables(env, LOGGER);
        executeSeedOnly();
    }

    private void executeSeedOnly() {
        if (CREATE_JSON_MOCKS) {
            List<ParamApp> paramsApp = TransactionUtilsMock.createParamsAppMock();
            List<Customer> customers = TransactionUtilsMock.createCustomerListMock();
            List<Wallet> wallets = TransactionUtilsMock.createWalletListMock(customers);
            List<LoginAuth> loginAuths = TransactionUtilsMock.createLoginAuthListMock(wallets);
            
            // Aplica criptografia REAL antes de exportar e persistir
            encriptAccessKeyLoginAuthListMock(loginAuths);

            TransactionMockResult trnMockResult = TransactionUtilsMock.createTransactionListMock(wallets);

            dtPersistenceService.exportToJson(paramsApp, FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP);
            dtPersistenceService.exportToJson(customers, FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER);
            dtPersistenceService.exportToJson(wallets, FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET);
            dtPersistenceService.exportToJson(loginAuths, FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH);
            dtPersistenceService.exportToJson(trnMockResult.getTransactions(), FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_TRANSACTION);
            dtPersistenceService.exportToJson(trnMockResult.getMovements(), FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_MOVIMENT);
            dtPersistenceService.exportToJson(trnMockResult.getDepositSenders(), FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_DEPOSIT_SENDER);

            runDatabaseSeeder();                
        }
    }

    private void encriptAccessKeyLoginAuthListMock(List<LoginAuth> loginAuths) {
        for (LoginAuth loginAuth : loginAuths) {
            String rawAccessKey = loginAuth.getAccessKey();
            // passwordEncoder real (ex: BCrypt) em ação
            String encodedAccessKey = passwordEncoder.encode(rawAccessKey);
            loginAuth.setAccessKey(encodedAccessKey);
        }
    }

    protected void runDatabaseSeeder() {
        LOGGER.info(LogMarkers.LOG, "Iniciando processo de Seed no banco de dados...");
        SeedOrderConfig config = new SeedOrderConfig();
        SeedExecutor executor = new SeedExecutor(context);
        SeedRunner runner = new SeedRunner(executor, config, seederEnabled);
        runner.runSeed();
        LOGGER.info(LogMarkers.LOG, "Seed executado com sucesso.");
    }
}
