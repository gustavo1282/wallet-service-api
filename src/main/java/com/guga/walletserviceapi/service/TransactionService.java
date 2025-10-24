package com.guga.walletserviceapi.service;

import com.guga.walletserviceapi.config.ResourceBadRequestException;
import com.guga.walletserviceapi.config.ResourceNotFoundException;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.TransactionType;
import com.guga.walletserviceapi.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletService walletService;

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + String.valueOf(id)));
    }

    public Transaction salvar(Transaction transaction) {

        if (transaction.getWalletId() == null) {
            throw new ResourceBadRequestException("The transaction does not contain a valid wallet");
        }

        Wallet wallet = walletService.getWalletById(transaction.getWalletId());

        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0.00) {
            throw new ResourceBadRequestException("The deposit amount must be greater than zero");
        }

        BigDecimal newCurrentBalance = applyOperationType(transaction.getTransactionType(), wallet.getCurrentBalance(), transaction.getAmount());

        transaction.setPreviousBalance(wallet.getCurrentBalance());
        transaction.setCurrentBalance(newCurrentBalance);
        transaction.setWallet(wallet);

        Transaction newTransaction = transactionRepository.save(transaction);


        newTransaction.getWallet().setPreviousBalance(wallet.getCurrentBalance());
        newTransaction.getWallet().setCurrentBalance(newCurrentBalance);

        walletService.updateWallet(wallet.getWalletId(), newTransaction.getWallet());

        return newTransaction;
    }

    private BigDecimal applyOperationType(
            TransactionType transactionType,
            BigDecimal currentBalance,
            BigDecimal amount) {

        BigDecimal newAmount = BigDecimal.ZERO;
        switch (transactionType) {
            case DEPOSIT -> {
                newAmount = currentBalance.add(amount);
            }
            case WITHDRAW -> {
                newAmount = currentBalance.subtract(amount);
                if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResourceBadRequestException("Insufficient funds for withdrawal.");
                }
            }
            case TRANSFER -> {
                newAmount = currentBalance.subtract(amount);
                if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResourceBadRequestException("Insufficient funds for transfer.");
                }
            }
        }
        return newAmount;
    }

    public Page<Transaction>  getAllTransactions(Pageable pageable) {
        Page<Transaction> resultData = transactionRepository.findAll(pageable);

        if (resultData.getContent().isEmpty()) {
            throw new ResourceNotFoundException("No transactions found");
        }

        return resultData;
    }

    public Page<Transaction>  findByWalletWalletIdAndCreatedAtBetween(
            Long walletId, LocalDateTime dtStart, LocalDateTime dtEnd,
            Pageable pageable) {

        Page<Transaction> resultData = transactionRepository.findByWalletWalletIdAndCreatedAtBetween(walletId, dtStart, dtEnd, pageable);

        if (resultData.getContent().isEmpty()) {
            throw new ResourceNotFoundException("No transactions found");
        }

        return resultData;
    }

}
