package com.guga.walletserviceapi.controller;

import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.service.CustomerService;
import com.guga.walletserviceapi.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("${controller.path.base}/wallet-operator")
@Tag(name = "WalletOperator", description = "Endpoints for managing Wallet Operators")
public class WalletOperatorController {

    @Autowired
    WalletService walletService;

    @Autowired
    CustomerService customerService;

    @Operation(summary = "Get Wallet by ID", description = "Retrieves a Wallet by their ID provided in the request body.")
    @GetMapping("/wallet/{id}")
    public ResponseEntity<Wallet> getWalletById(
            @Parameter(description = "ID of the Wallet", required = true)
            @PathVariable(name = "walletId", required = true)
            Long id) {

        Wallet wallet = walletService.getWalletById(id);
        return new ResponseEntity<>(wallet, HttpStatus.OK);

    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get Transaction by ID", description = "Retrieves a Transaction by their ID provided in the request body.")
    public ResponseEntity<Transaction> getTransactionById(
            @Parameter(description = "ID of the Transaction", required = true)
            @PathVariable(name = "transactionId", required = true)
            Long id) {

        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @GetMapping("/transactions/{walletId}/{date}")
    @Operation(summary = "Get all Transactions", description = "Retrieves all Transactions.")
    public ResponseEntity<List<Transaction>> getTransactionFilter(
            @Parameter(description = "ID of the wallet", required = true)
            @PathVariable(name = "walletId", required = true)
            Long walletId,

            @Parameter(description = "Date of the transaction", required = true)
            @PathVariable(name = "date", required = true)
            LocalDate date
    ) {

        List<Transaction> transactions = new ArrayList<Transaction>() ;
//        for (int i = 0; i < 10; i++) {
//            transactions.add(Transaction.newSampleTransaction());
//        }
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }


}
