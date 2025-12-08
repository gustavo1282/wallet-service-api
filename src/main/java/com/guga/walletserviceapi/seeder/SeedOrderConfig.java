package com.guga.walletserviceapi.seeder;

import java.util.List;

import org.springframework.stereotype.Component;

import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.Wallet;

@Component
public class SeedOrderConfig {

    public List<SeedDefinition> orderedSeeds() {

        String folder = FileUtils.FOLDER_DEFAULT_FILE_JSON;

        return List.of(
            new SeedDefinition(folder + FileUtils.JSON_FILE_PARAMS_APP , ParamApp.class),

            // 1) Tabelas de domínio
            new SeedDefinition(folder + FileUtils.JSON_FILE_CUSTOMER, Customer.class),
            new SeedDefinition(folder + FileUtils.JSON_FILE_LOGIN_AUTH, LoginAuth.class),
            new SeedDefinition(folder + FileUtils.JSON_FILE_WALLET, Wallet.class),
            new SeedDefinition(folder + FileUtils.JSON_FILE_DEPOSIT_SENDER, DepositSender.class),

            new SeedDefinition(folder + FileUtils.JSON_FILE_MOVIMENT, MovementTransaction.class),
            new SeedDefinition(folder + FileUtils.JSON_FILE_TRANSACTION, Transaction.class)
        );
    }

}
