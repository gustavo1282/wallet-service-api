package com.guga.walletserviceapi.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import com.guga.walletserviceapi.audit.AuditLogContext;
import com.guga.walletserviceapi.audit.AuditLogger;
import com.guga.walletserviceapi.dto.transaction.TransactionMapper;
import com.guga.walletserviceapi.dto.transaction.TransactionResponseDTO;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.service.CustomerService;
import com.guga.walletserviceapi.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/wallet-operator")
@Tag(name = "WalletOperator", description = "Centralized Hub for Customer, Wallet and Transaction operations")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class WalletOperatorController {

    private static final Logger LOGGER = LogManager.getLogger(WalletOperatorController.class);

    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final JwtAuthenticatedUserProvider authUserProvider;
    private final TransactionMapper transactionMapper;

    // =====================================================
    // ME CONTEXT - TRANSACTION EXTRACTS
    // =====================================================

    @Operation(
        operationId = "walletoperator_01_get_my_recent_transactions",
        summary = "Get my recent transactions (all types)",
        description = "Returns a paginated list of recent transactions for the authenticated user's wallet."
    )
    @GetMapping("/me/transactions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> getMyRecentTransactions() {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        AuditLogger.log("TRANSACTION_LIST_ALL_ME", auditCtx);

        Pageable pageable = GlobalHelper.getDefaultPageable();

        Page<Transaction> trnResult = transactionService.getTransactionsByLimitMax(auditCtx.getWalletId(), pageable);

        return (trnResult != null)
            ? ResponseEntity.ok(trnResult.map(transactionMapper::toDto))
            : ResponseEntity.ok(Page.empty());
    }

    @Operation(
        operationId = "walletoperator_02_list_my_deposits",
        summary = "List my deposits",
        description = "Returns a paginated list of DEPOSIT operations for the authenticated user's wallet."
    )
    @GetMapping("/me/transactions/deposits")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyDeposits() {
        return listMyTransactionsByOperation(OperationType.DEPOSIT);
    }

    @Operation(
        operationId = "walletoperator_03_list_my_withdraws",
        summary = "List my withdraws",
        description = "Returns a paginated list of WITHDRAW operations for the authenticated user's wallet."
    )
    @GetMapping("/me/transactions/withdraws")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyWithdraws() {
        return listMyTransactionsByOperation(OperationType.WITHDRAW);
    }

    @Operation(
        operationId = "walletoperator_04_list_my_transfers_sent",
        summary = "List my transfers sent",
        description = "Returns a paginated list of TRANSFER_SEND operations for the authenticated user's wallet."
    )
    @GetMapping("/me/transactions/transfers-sent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyTransfersSent() {
        return listMyTransactionsByOperation(OperationType.TRANSFER_SEND);
    }

    @Operation(
        operationId = "walletoperator_05_list_my_transfers_received",
        summary = "List my transfers received",
        description = "Returns a paginated list of TRANSFER_RECEIVED operations for the authenticated user's wallet."
    )
    @GetMapping("/me/transactions/transfers-received")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyTransfersReceived() {
        return listMyTransactionsByOperation(OperationType.TRANSFER_RECEIVED);
    }

    @Operation(
        operationId = "walletoperator_06_search_my_transactions_by_period",
        summary = "Search my transactions by period",
        description = "Searches transactions for the authenticated user's wallet between startDate and endDate. "
                    + "If endDate is not provided, it defaults to now."
    )
    @GetMapping("/me/transactions/period")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> searchMyTransactionsByPeriod(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        Pageable pageable = GlobalHelper.getDefaultPageable();

        AuditLogger.log("TRANSACTION_LIST_PERIOD_ME", auditCtx);
        return ResponseEntity.ok(
            transactionService
                .findByWalletIdAndCreatedAtBetween(auditCtx.getWalletId(), start, end, pageable)
                .map(transactionMapper::toDto)
        );
    }

    // =====================================================
    // ADMIN CONTEXT - MAINTENANCE & UPLOADS (ROLE_ADMIN)
    // =====================================================

    @Operation(
        operationId = "walletoperator_07_admin_upload_customers_csv",
        summary = "Admin: Upload customers CSV",
        description = "Imports customers from a CSV file. Admin-only operation."
    )
    @PostMapping("/uploads/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadCustomers(@RequestParam MultipartFile file) {
        customerService.importCustomers(file);
        return ResponseEntity.ok("Customers uploaded successfully");
    }

    @Operation(
        operationId = "walletoperator_08_admin_upload_transactions_csv",
        summary = "Admin: Upload transactions CSV",
        description = "Imports transactions from a CSV file. Admin-only operation."
    )
    @PostMapping("/uploads/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadTransactions(@RequestParam MultipartFile file) {
        transactionService.importTransactions(file);
        return ResponseEntity.ok("Transactions uploaded successfully");
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    private ResponseEntity<Page<TransactionResponseDTO>> listMyTransactionsByOperation(OperationType operation) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        AuditLogger.log("TRANSACTION_LIST_" + operation.name() + "_ME", auditCtx);

        Pageable pageable = GlobalHelper.getDefaultPageable();

        Page<Transaction> trnResult = transactionService.filterTransactionByWalletIdAndOperationType(
            auditCtx.getWalletId(), operation, pageable
        );

        // (Opcional) log: como é um endpoint "hub", pode ser útil
        LOGGER.info(LogMarkers.LOG, "WALLET_OPERATOR_LIST | walletId={} operation={}", auditCtx.getWalletId(), operation);

        return (trnResult != null)
            ? ResponseEntity.ok(trnResult.map(transactionMapper::toDto))
            : ResponseEntity.ok(Page.empty());
    }
}