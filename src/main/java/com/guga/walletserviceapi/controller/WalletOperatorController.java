package com.guga.walletserviceapi.controller;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.audit.AuditLogContext;
import com.guga.walletserviceapi.audit.AuditLogger;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.service.CustomerService;
import com.guga.walletserviceapi.service.DepositSenderService;
import com.guga.walletserviceapi.service.MovementTransactionService;
import com.guga.walletserviceapi.service.TransactionService;
import com.guga.walletserviceapi.service.WalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${controller.path.base}/wallet-operator")
@Tag(name = "Wallet Operator", description = "Centralized Hub for Customer, Wallet and Transaction operations")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class WalletOperatorController {

    private static final Logger LOGGER = LogManager.getLogger(WalletOperatorController.class);

    private final WalletService walletService;
    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final MovementTransactionService movementTransactionService;
    private final DepositSenderService depositSenderService;
    private final JwtAuthenticatedUserProvider authUserProvider;

    @Value("${spring.data.web.pageable.default-page-size}")
    private int defaultPageSize;

    // // =====================================================
    // // ME CONTEXT - CUSTOMER & WALLET
    // // =====================================================

    // @Operation(summary = "Get my personal data (Customer)")
    // @GetMapping("/me/customer")
    // @PreAuthorize("hasRole('USER')")
    // public ResponseEntity<Customer> getMyCustomerData() {
    //     AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
    //     AuditLogger.log("CUSTOMER_GET_ME", auditCtx);
    //     // Busca os dados do cliente dono do token
    //     return ResponseEntity.ok(customerService.getCustomerById(auditCtx.getCustomerId()));
    // }

    // @Operation(summary = "Get my wallet details and balance")
    // @GetMapping("/me/wallet")
    // @PreAuthorize("hasRole('USER')")
    // public ResponseEntity<Wallet> getMyWalletDetails() {
    //     AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
    //     AuditLogger.log("WALLET_GET_ME", auditCtx);
    //     return ResponseEntity.ok(walletService.getWalletById(auditCtx.getWalletId()));
    // }

    // =====================================================
    // ME CONTEXT - TRANSACTION EXTRACTS (LIMIT 100)
    // =====================================================

    @Operation(summary = "Get my last 50 transactions (All types)")
    @GetMapping("/me/transactions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Transaction>> getMyRecentTransactions() {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        AuditLogger.log("TRANSACTION_LIST_ALL_ME", auditCtx);
                
        Pageable pageable = GlobalHelper.getDefaultPageable();

        Page<Transaction> trnResult = transactionService.getTransactionsByLimitMax(auditCtx.getWalletId(), pageable);
        return (trnResult != null) ? ResponseEntity.ok(trnResult) : ResponseEntity.ok(Page.empty());
    }

    @Operation(summary = "List my last 100 deposits")
    @GetMapping("/me/transactions/deposits")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Transaction>> listMyDeposits() {
        return listMyTransactionsByOperation(OperationType.DEPOSIT);
    }

    @Operation(summary = "List my last 100 withdraws")
    @GetMapping("/me/transactions/withdraws")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Transaction>> listMyWithdraws() {
        return listMyTransactionsByOperation(OperationType.WITHDRAW);
    }

    @Operation(summary = "List my last 100 transfers sent")
    @GetMapping("/me/transactions/transfers-sent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Transaction>> listMyTransfersSent() {
        return listMyTransactionsByOperation(OperationType.TRANSFER_SEND);
    }

    @Operation(summary = "List my last 100 transfers received")
    @GetMapping("/me/transactions/transfers-received")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Transaction>> listMyTransfersReceived() {
        return listMyTransactionsByOperation(OperationType.TRANSFER_RECEIVED);
    }

    @Operation(summary = "Search my transactions by period")
    @GetMapping("/me/transactions/period")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Transaction>> searchMyTransactionsByPeriod(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
        //@RequestParam(defaultValue = "0") int page
    ) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        Pageable pageable = GlobalHelper.getDefaultPageable();
        
        AuditLogger.log("TRANSACTION_LIST_PERIOD_ME", auditCtx);
        return ResponseEntity.ok(transactionService.findByWalletIdAndCreatedAtBetween(auditCtx.getWalletId(), start, end, pageable));
    }

    // =====================================================
    // ADMIN CONTEXT - MAINTENANCE & UPLOADS (ROLE_ADMIN)
    // =====================================================

    @Operation(summary = "Admin: Upload customers CSV")
    @PostMapping("/uploads/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadCustomers(@RequestParam MultipartFile file) {
        customerService.importCustomers(file);
        return ResponseEntity.ok("Customers uploaded successfully");
    }

    @Operation(summary = "Admin: Upload transactions CSV")
    @PostMapping("/uploads/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadTransactions(@RequestParam MultipartFile file) {
        transactionService.importTransactions(file);
        return ResponseEntity.ok("Transactions uploaded successfully");
    }

    // ... (Mantendo os outros métodos de upload: wallets, movements, deposit-senders conforme sua classe original)

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    private ResponseEntity<Page<Transaction>> listMyTransactionsByOperation(OperationType operation) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        AuditLogger.log("TRANSACTION_LIST_" + operation.name() + "_ME", auditCtx);
        
        Pageable pageable = GlobalHelper.getDefaultPageable();
        
        Page<Transaction> trnResult = transactionService.filterTransactionByWalletIdAndOperationType(
            auditCtx.getWalletId(), operation, pageable);

        return (trnResult != null) ? ResponseEntity.ok(trnResult) : ResponseEntity.ok(Page.empty());

    }

    private URI buildLocation(Long transactionId) {
        return ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(transactionId).toUri();
    }
}