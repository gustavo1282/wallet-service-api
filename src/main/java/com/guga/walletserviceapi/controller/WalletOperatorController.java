package com.guga.walletserviceapi.controller;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.model.FileUploadWrapper;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.request.CreateDepositRequest;
import com.guga.walletserviceapi.model.request.CreateTransferRequest;
import com.guga.walletserviceapi.model.request.CreateWithdrawRequest;
import com.guga.walletserviceapi.service.CustomerService;
import com.guga.walletserviceapi.service.DepositSenderService;
import com.guga.walletserviceapi.service.MovementTransactionService;
import com.guga.walletserviceapi.service.TransactionService;
import com.guga.walletserviceapi.service.WalletService;
import com.guga.walletserviceapi.service.common.DataImportService;
import com.guga.walletserviceapi.service.common.ImportSummary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${controller.path.base}/wallet-operator")
@Tag(name = "Wallet Operator", description = "Administrative wallet operations")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class WalletOperatorController {

    private static final Logger LOGGER = LogManager.getLogger(WalletOperatorController.class);

    private final WalletService walletService;
    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final MovementTransactionService movementTransactionService;
    private final DepositSenderService depositSenderService;
    private final DataImportService importService;

    @Value("${spring.data.web.pageable.default-page-size}")
    private int defaultPageSize;

    // =====================================================
    // WALLET
    // =====================================================

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<Wallet> getWalletById(@PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.getWalletById(walletId));
    }

    // =====================================================
    // TRANSACTIONS
    // =====================================================

    @GetMapping("/transactions/{walletId}/last")
    public ResponseEntity<Page<Transaction>> getLastTransactions(
        @PathVariable Long walletId,
        @RequestParam(defaultValue = "0") int page
    ) {

        Pageable pageable = PageRequest.of(
            page,
            10,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        return ResponseEntity.ok(
            transactionService.getLast10Transactions(walletId, pageable)
        );
    }

    @GetMapping("/transactions/{walletId}/period")
    public ResponseEntity<Page<Transaction>> searchTransactionsByPeriod(
        @PathVariable Long walletId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "0") int page
    ) {

        Pageable pageable = PageRequest.of(
            page,
            defaultPageSize,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("transactionId")
            )
        );

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate != null
            ? endDate.atTime(LocalTime.MAX)
            : LocalDate.now().atTime(LocalTime.MAX);

        return ResponseEntity.ok(
            transactionService.findByWalletIdAndCreatedAtBetween(
                walletId, start, end, pageable
            )
        );
    }

    // =====================================================
    // OPERATIONAL TRANSACTIONS
    // =====================================================

    @PostMapping("/transactions/{walletId}/withdraw")
    public ResponseEntity<Transaction> withdraw(
        @PathVariable Long walletId,
        @RequestBody CreateWithdrawRequest request
    ) {

        Transaction withdraw = transactionService
            .saveWithdrawMoney(walletId, request.getAmount());

        return ResponseEntity.created(buildLocation(withdraw.getTransactionId()))
            .header("X-Trace-Id", ThreadContext.get("traceId"))
            .body(withdraw);
    }

    @PostMapping("/transactions/{walletId}/deposit")
    public ResponseEntity<Transaction> deposit(
        @PathVariable Long walletId,
        @RequestBody CreateDepositRequest request
    ) {

        Transaction deposit = transactionService.saveDepositMoney(
            walletId,
            request.getAmount(),
            request.getCpfSender(),
            request.getTerminalId(),
            request.getSenderName()
        );

        return ResponseEntity.created(buildLocation(deposit.getTransactionId()))
            .header("X-Trace-Id", ThreadContext.get("traceId"))
            .body(deposit);
    }

    @PostMapping("/transactions/{walletId}/transfer")
    public ResponseEntity<Transaction> transfer(
        @PathVariable Long walletId,
        @RequestBody CreateTransferRequest request
    ) {

        Transaction transfer = transactionService.saveTransferMoneySend(
            request.getWalletIdSend(),
            request.getWalletIdReceived(),
            request.getAmount()
        );

        return ResponseEntity.created(buildLocation(transfer.getTransactionId()))
            .header("X-Trace-Id", ThreadContext.get("traceId"))
            .body(transfer);
    }

    // =====================================================
    // UPLOADS (ADMIN ONLY)
    // =====================================================

    @Operation(summary = "Upload customers CSV")
    @PostMapping("/uploads/customers")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            mediaType = "multipart/form-data",
            schema = @Schema(implementation = FileUploadWrapper.class)
        )
    )
    public ResponseEntity<String> uploadCustomers(@RequestParam MultipartFile file) {
        ImportSummary summary = customerService.importCustomers(file);
        return ResponseEntity.ok("Customers uploaded successfully");
    }

    @Operation(summary = "Upload wallets CSV")
    @PostMapping("/uploads/wallets")
    public ResponseEntity<String> uploadWallets(@RequestParam MultipartFile file) {
        ImportSummary summary = walletService.importWallets(file);
        return ResponseEntity.ok("Wallets uploaded successfully");
    }

    @Operation(summary = "Upload transactions CSV")
    @PostMapping("/uploads/transactions")
    public ResponseEntity<String> uploadTransactions(@RequestParam MultipartFile file) {
        ImportSummary summary = transactionService.importTransactions(file);
        return ResponseEntity.ok("Transactions uploaded successfully");
    }

    @Operation(summary = "Upload movements CSV")
    @PostMapping("/uploads/movements")
    public ResponseEntity<String> uploadMovements(@RequestParam MultipartFile file) {
        ImportSummary summary = movementTransactionService.importMovementTransactions(file);
        return ResponseEntity.ok("Movements uploaded successfully");
    }

    @Operation(summary = "Upload deposit senders CSV")
    @PostMapping("/uploads/deposit-senders")
    public ResponseEntity<String> uploadDepositSenders(@RequestParam MultipartFile file) {
        ImportSummary summary = depositSenderService.importDeposits(file);
        return ResponseEntity.ok("Deposit senders uploaded successfully");
    }

    // =====================================================
    // UTIL
    // =====================================================

    private URI buildLocation(Long transactionId) {
        return ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(transactionId)
            .toUri();
    }
}
