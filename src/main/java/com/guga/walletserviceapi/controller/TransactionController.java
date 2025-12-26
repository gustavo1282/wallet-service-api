package com.guga.walletserviceapi.controller;


import java.math.BigDecimal;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.TransferMoneySend;
import com.guga.walletserviceapi.model.enums.StatusTransaction;
import com.guga.walletserviceapi.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${controller.path.base}/transactions")
@Tag(name = "Transaction", description = "Endpoints for managing transactions")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Value("${spring.data.web.pageable.default-page-size}")
    private int defaultPageSize;

    @Operation(summary = "Create a new transaction of type DEPOSIT",
            description = "Creates a new transaction with the data provided in the request body.")
    @PostMapping(value = "/transaction", params = "type=DEPOSIT")
    public ResponseEntity<DepositMoney> createNewDepositMoneyTransaction(@RequestParam Long walletId,
        @RequestParam BigDecimal amount,
        @RequestParam String cpfSender,
        @RequestParam String terminalId,
        @RequestParam String senderName
        )
    {
        DepositMoney depositCreated = transactionService.saveDepositMoney(walletId, amount,
                cpfSender, terminalId, senderName);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(depositCreated.getTransactionId())
                .toUri();

        return ResponseEntity.created(location).body(depositCreated);
    }

    @Operation(summary = "createNewWithdrawMoneyTransaction >> Create a new transaction of type WITHDRAW",
            description = "Creates a new transaction with the data provided in the request body.")
    @PostMapping(value = "/transaction", params = "type=WITHDRAW")
    public ResponseEntity<Transaction> createNewWithdrawMoneyTransaction(
            @RequestParam Long walletId,
            @RequestParam BigDecimal amount) {
//        @RequestBody @Valid WithdrawMoney withdrawMoney

        Transaction createdTransaction = transactionService.saveWithdrawMoney(walletId, amount);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTransaction.getTransactionId())
                .toUri();

        return ResponseEntity.created(location).body(createdTransaction);
    }

    @Operation(summary = "Create a new transaction of type TRANSFER_SEND",
            description = "Creates a new transaction with the data provided in the request body.")
    @PostMapping(value = "/transaction", params = "type=TRANSFER_SEND")
    public ResponseEntity<TransferMoneySend> createNewTransferMoneySend(
            @RequestParam Long walletIdSend,
            @RequestParam Long walletIdReceived,
            @RequestParam BigDecimal amount
            ) {

        TransferMoneySend transferMoneyCreated = transactionService
                .saveTransferMoneySend(walletIdSend, walletIdReceived, amount);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(transferMoneyCreated.getTransactionId())
                .toUri();

        return ResponseEntity.created(location).body(transferMoneyCreated);
    }

    @Operation(summary = "Get transaction by ID", description = "Retrieves a transaction by their ID provided in the request body.")
    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long transactionId) {

        Transaction transaction = transactionService.getTransactionById(transactionId);

        return new ResponseEntity<>(transaction, HttpStatus.OK);

    }

    @Operation(summary = "Get transactions by Wallet ID",
            description = "Retrieves a list transaction by Wallet ID provided in the request body.")
    @GetMapping(value = "/search-wallet", params = { "walletId", "!typeTransaction" })
    public ResponseEntity<Page<Transaction>> getTransactionByWalletId(
            @RequestParam(required = true) Long walletId,
            @RequestParam(defaultValue = "25") int page
            ) 
        {

        Pageable pageable = PageRequest.of(page, defaultPageSize,
            Sort.by(
                Sort.Order.asc("walletId"),
                Sort.Order.asc("transactionId")
            )
        );
        
        Page<Transaction> pageTransaction = transactionService.getTransactionByWalletId(walletId, pageable);

        return new ResponseEntity<>(pageTransaction, HttpStatus.OK);
    }

    @Operation(summary = "Get transactions by Wallet ID and Status",
            description = "Retrieves a list transaction by Wallet ID provided in the request body.")
    @GetMapping(value = "/list", params = { "walletId", "typeTransaction" })
    public ResponseEntity<Page<Transaction>> getTransactionByWalletIdAndProcessType(
            @RequestParam(required = true) Long walletId,
            @RequestParam(required = false) StatusTransaction typeTransaction,
            @RequestParam(defaultValue = "0") int page
            ) 
    {

        Pageable pageable = PageRequest.of(page, defaultPageSize,
            Sort.by(
                Sort.Order.asc("walletId"),
                Sort.Order.asc("transactionId")
            )
        );

        Page<Transaction> pageTransaction = transactionService
                .filterTransactionByWalletIdAndProcessType(walletId, typeTransaction, pageable);

        return new ResponseEntity<>(pageTransaction, HttpStatus.OK);
    }

}