package com.guga.walletserviceapi.controller;


import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("${controller.path.base}/transactions")
@Tag(name = "Transaction", description = "Endpoints for managing transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Operation(summary = "Create a new transaction", description = "Creates a new transaction with the data provided in the request body.")
    @PostMapping("/transaction")
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
        Transaction createdTransaction = transactionService.salvar(transaction);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTransaction.getTransactionId())
                .toUri();

        return ResponseEntity.created(location).body(createdTransaction);
    }

    @Operation(summary = "Get all transactions", description = "Retrieves all transactions.")
    @GetMapping("/list")
    public ResponseEntity<List<Transaction>> getAllTransactions(Pageable pageable) {

        Page<Transaction> resultTransaction = transactionService.getAllTransactions(pageable);

        if (resultTransaction == null || resultTransaction.isEmpty() || resultTransaction.stream().toList().isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(resultTransaction.stream().toList(), HttpStatus.OK);
    }

    @Operation(summary = "Get transaction by ID", description = "Retrieves a transaction by their ID provided in the request body.")
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        Transaction transaction = transactionService.getTransactionById(id);
        return new ResponseEntity<>(transaction, HttpStatus.OK);
    }

    @Operation(summary = "Search transactions by Wallet ID within a period",
            description = "Returns a paginated list of transactions for a specific wallet. " +
                    "optionally filtered for a period of time. " +
                    "The default sorting is by ascending ‘wallet_id’ and descending ‘created_at’.")
    @GetMapping("/by-wallet/{walletId}")
    public ResponseEntity<List<Transaction>> findByWalletWalletIdAndCreatedAtBetween(
            @PathVariable Long walletId,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dtStart,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dtEnd,
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "wallet_id", direction = Sort.Direction.ASC),
                    @SortDefault(sort = "created_At", direction = Sort.Direction.DESC)
            }) Pageable pageable) {

        dtEnd.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        Page<Transaction> resultTransaction = transactionService
                .findByWalletWalletIdAndCreatedAtBetween(walletId, dtStart, dtEnd, pageable);

        if (resultTransaction == null || resultTransaction.isEmpty() || resultTransaction.stream().toList().isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(resultTransaction.stream().toList(), HttpStatus.OK);

    }

}