package com.guga.walletserviceapi.controller;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.audit.AuditLogContext;
import com.guga.walletserviceapi.audit.AuditLogger;
import com.guga.walletserviceapi.dto.transaction.TransactionDepositDTO;
import com.guga.walletserviceapi.dto.transaction.TransactionMapper;
import com.guga.walletserviceapi.dto.transaction.TransactionResponseDTO;
import com.guga.walletserviceapi.dto.transaction.TransactionTransferDTO;
import com.guga.walletserviceapi.dto.transaction.TransactionWithdrawDTO;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.TransferMoneySend;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.StatusTransaction;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transaction", description = "Endpoints for managing transactions")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class TransactionController {

    private static final Logger LOGGER = LogManager.getLogger(TransactionController.class);

    private final TransactionService transactionService;
    private final JwtAuthenticatedUserProvider authUserProvider;
    private final TransactionMapper transactionMapper;


    @Operation(
        operationId = "transaction_01_list_my_transactions",
        summary = "List my transactions",
        description = "Retrieves transactions for the authenticated user's wallet."
    )
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyTransactions(
        @RequestParam(required = false) StatusTransaction status
    ) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG, "LIST_MY_TRANSACTIONS | walletId={} status={}", walletId, status);

        Pageable pageable = GlobalHelper.getDefaultPageable();
        Page<TransactionResponseDTO> result = transactionService
            .filterTransactionByWalletIdAndProcessType(walletId, status, pageable)
            .map(transactionMapper::toDto);

        AuditLogger.log(
            "TRANSACTION_LIST_ME",
            auditCtx.toBuilder().info("rows=" + result.getNumberOfElements()).build()
        );
        return ResponseEntity.ok(result);
    }


    @Operation(
        operationId = "transaction_02_get_my_transaction_by_id",
        summary = "Get details of my own transaction",
        description = "Returns details of a transaction only if it belongs to the authenticated user's wallet."
    )
    @GetMapping("/me/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponseDTO> getMyTransactionById(@PathVariable Long id) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Transaction transaction = transactionService.getTransactionById(id);

        // REGRA DE SEGURANÇA 1: Validar se a transação pertence ao usuário logado
        if (!transaction.getWalletId().equals(auditCtx.getWalletId())) {
            LOGGER.warn("SECURITY ALERT | Unauthorized access attempt | userWallet={} targetTransactionId={}",
                        auditCtx.getWalletId(), id);
            throw new ResourceBadRequestException("Access denied: This transaction does not belong to your wallet.");
        }

        AuditLogger.log(
            "TRANSACTION_GET_BY_ID_ME",
            auditCtx.toBuilder().info("transactionId=" + id).build()
        );
        return ResponseEntity.ok(transactionMapper.toDto(transaction));
    }

    @Operation(
        operationId = "transaction_03_list_my_deposits_last_150",
        summary = "List my last 150 deposits",
        description = "Returns the last 150 DEPOSIT operations for the authenticated user's wallet."
    )
    @GetMapping("/me/deposits")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyDeposits() {
        return listMyTransactionsByOperation(OperationType.DEPOSIT);
    }

    @Operation(
        operationId = "transaction_04_list_my_withdraws_last_150",
        summary = "List my last 150 withdraws",
        description = "Returns the last 150 WITHDRAW operations for the authenticated user's wallet."
    )
    @GetMapping("/me/withdraws")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyWithdraws() {
        return listMyTransactionsByOperation(OperationType.WITHDRAW);
    }

    @Operation(
        operationId = "transaction_05_list_my_transfers_sent_last_150",
        summary = "List my last 150 sent transfers",
        description = "Returns the last 150 TRANSFER_SEND operations for the authenticated user's wallet."
    )
    @GetMapping("/me/transfers-send")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyTransfersSent() {
        return listMyTransactionsByOperation(OperationType.TRANSFER_SEND);
    }

    @Operation(
        operationId = "transaction_06_list_my_transfers_received_last_150",
        summary = "List my last 150 received transfers",
        description = "Returns the last 150 TRANSFER_RECEIVED operations for the authenticated user's wallet."
    )
    @GetMapping("/me/transfers-received")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyTransfersReceived() {
        return listMyTransactionsByOperation(OperationType.TRANSFER_RECEIVED);
    }    

    @Operation(
        operationId = "transaction_07_create_deposit",
        summary = "Deposit money",
        description = "Creates a DEPOSIT transaction for the authenticated user's wallet."
    )
    @PostMapping("/deposit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponseDTO> createDeposit(
        @RequestBody @Valid TransactionDepositDTO dto
    ) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG, "DEPOSIT | walletId={} amount={}", walletId, dto.amount());
        AuditLogger.log("TRANSACTION_DEPOSIT [START]", auditCtx);

        DepositMoney deposit = transactionService.saveDepositMoney(
            walletId, dto.amount(), dto.cpfSender(), dto.terminalId(), dto.senderName()
        );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{transactionId}")
            .buildAndExpand(deposit.getTransactionId())
            .toUri();

        AuditLogger.log(
            "TRANSACTION_DEPOSIT [SUCCESS]",
            auditCtx.toBuilder().info("transactionId=" + deposit.getTransactionId()).build()
        );
        return ResponseEntity.created(location).body(transactionMapper.toDto(deposit));
    }

    @Operation(
        operationId = "transaction_08_create_withdraw",
        summary = "Withdraw money",
        description = "Creates a WITHDRAW transaction for the authenticated user's wallet."
    )
    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponseDTO> createWithdraw(
        @RequestBody @Valid TransactionWithdrawDTO dto
    ) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG, "WITHDRAW | walletId={} amount={}", walletId, dto.amount());
        AuditLogger.log("TRANSACTION_WITHDRAW [START]", auditCtx);

        Transaction transaction = transactionService.saveWithdrawMoney(walletId, dto.amount());

        AuditLogger.log(
            "TRANSACTION_WITHDRAW [SUCCESS]",
            auditCtx.toBuilder().info("transactionId=" + transaction.getTransactionId()).build()
        );
        return ResponseEntity.ok(transactionMapper.toDto(transaction));
    }

    @Operation(
        operationId = "transaction_09_create_transfer",
        summary = "Transfer money",
        description = "Creates a TRANSFER transaction from the authenticated user's wallet."
    )
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponseDTO> createTransfer(
        @RequestBody @Valid TransactionTransferDTO dto
    ) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletIdSend = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG, "TRANSFER | fromWallet={} toWallet={} amount={}",
            walletIdSend, dto.walletIdReceived(), dto.amount()
        );
        AuditLogger.log("TRANSACTION_TRANSFER [START]", auditCtx);

        TransferMoneySend transfer =
            transactionService.saveTransferMoneySend(walletIdSend, dto.walletIdReceived(), dto.amount());

        AuditLogger.log(
            "TRANSACTION_TRANSFER [SUCCESS]",
            auditCtx.toBuilder().info("transactionId=" + transfer.getTransactionId()).build()
        );
        return ResponseEntity.ok(transactionMapper.toDto(transfer));
    }

    @Operation(
        operationId = "transaction_10_get_by_id_admin",
        summary = "Get transaction by ID (ADMIN)",
        description = "Retrieves a transaction by ID. Admin-only operation."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(@PathVariable Long id) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG, "GET_TRANSACTION_BY_ID | transactionId={} admin={}",
            id, auditCtx.getUsername()
        );

        Transaction transaction = transactionService.getTransactionById(id);

        AuditLogger.log(
            "TRANSACTION_GET_BY_ID",
            auditCtx.toBuilder().info("transactionId=" + id).build()
        );

        return ResponseEntity.ok(transactionMapper.toDto(transaction));
    }

    @Operation(
        operationId = "transaction_11_list_by_wallet_admin",
        summary = "List transactions by wallet (ADMIN)",
        description = "Retrieves transactions by wallet ID. Admin-only operation."
    )
    @GetMapping("/wallet/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionResponseDTO>> listTransactionsByWallet(
        @PathVariable Long id,
        @RequestParam(required = false) StatusTransaction type
    ) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG, "LIST_TRANSACTIONS_BY_WALLET | walletId={} type={} admin={}",
            id, type, auditCtx.getUsername()
        );

        Pageable pageable = GlobalHelper.getDefaultPageable();
        Page<TransactionResponseDTO> result = transactionService
            .filterTransactionByWalletIdAndProcessType(id, type, pageable)
            .map(transactionMapper::toDto);

        AuditLogger.log(
            "TRANSACTION_LIST_BY_WALLET",
            auditCtx.toBuilder().info("walletId=" + id + ",rows=" + result.getNumberOfElements()).build()
        );

        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Page<TransactionResponseDTO>> listMyTransactionsByOperation(OperationType operation) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        Pageable limitPageable = PageRequest.of(0, 150, Sort.by(Sort.Direction.DESC, "createdAt", "transactionId"));
        LOGGER.info(LogMarkers.LOG, "LIST_MY_TRANSACTIONS | walletId={} operation={} limit=150", walletId, operation);

        String auditTag = "TRANSACTION_LIST_" + operation.name() + "_ME";
        Page<TransactionResponseDTO> result = transactionService
            .filterTransactionByWalletIdAndOperationType(walletId, operation, limitPageable)
            .map(transactionMapper::toDto);
        AuditLogger.log(
            auditTag,
            auditCtx.toBuilder().info("rows=" + result.getNumberOfElements()).build()
        );

        return ResponseEntity.ok(result);
    }


}
