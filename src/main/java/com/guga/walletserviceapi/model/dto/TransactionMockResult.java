package com.guga.walletserviceapi.model.dto;

import java.util.List;

import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.Transaction;

public class TransactionMockResult {

    private final List<Transaction> transactions;
    private final List<MovementTransaction> movements;
    private final List<DepositSender> depositSenders;

    public TransactionMockResult(
            List<Transaction> transactions,
            List<MovementTransaction> movements,
            List<DepositSender> depositSenders
    ) {
        this.transactions = transactions;
        this.movements = movements;
        this.depositSenders = depositSenders;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<MovementTransaction> getMovements() {
        return movements;
    }

    public List<DepositSender> getDepositSenders() {
        return depositSenders;
    }
}