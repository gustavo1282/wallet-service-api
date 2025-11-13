package com.guga.walletserviceapi.helpers;

import com.guga.walletserviceapi.model.*;
import com.guga.walletserviceapi.model.enums.CompareBigDecimal;
import com.guga.walletserviceapi.model.enums.StatusTransaction;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.model.enums.OperationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionUtils {

//    private static final boolean APPLY_FILTER_CUSTOMER_BY_STATUS = false;
//    private static final int RANGE_CUSTOMER_ID = 1000;
//    private static final int TOTAL_CUSTOMER_ID = 1050;
//    private static final int LIMIT_LIST_CUSTOMER = 30;
//
//    public static final boolean APPLY_FILTER_WALLET_BY_STATUS = false;
//    public static final int RANGE_WALLET_ID = 2000;
//    public static final int TOTAL_WALLET_ID = 2150;
//    public static final int LIMIT_LIST_WALLET = 80;
//
//    public static final int INI_TRANSACTION_ID = 1;
//    public static final int MAX_TRANSACTION_ID = 2000;
//
//    public static final int RANGE_TRANSACTION = 11000;
//    public static final int RANGE_TRANSFER_MONEY = 12000;
//    public static final int RANGE_DEPOSIT_SENDER = 12900;
//
//    public static final Double MONEY_MIN = 10D;
//    public static final Double MONEY_MAX = 800D;
//
    public static BigDecimal AMOUNT_MIN_TO_DEPOSIT = new BigDecimal(50);
    public static BigDecimal AMOUNT_MIN_TO_TRANSFER = new BigDecimal(50);

    public static DepositMoney generateDepositMoney(Wallet wallet, BigDecimal amount) {
        StatusTransaction statusTransaction = chekProcessTypeDeposit(amount, wallet);

        return DepositMoney.builder()
                .walletId(wallet.getWalletId())
                .createdAt(LocalDateTime.now())
                .statusTransaction( statusTransaction )
                .amount( amount )
                .previousBalance( wallet.getCurrentBalance( ))
                .currentBalance( wallet.getCurrentBalance().add(amount) )
                .operationType(OperationType.DEPOSIT)
                .build();
    }

    public static WithdrawMoney generateWithdraw(Wallet wallet, BigDecimal amount) {
        StatusTransaction processType = chekProcessTypeWithdraw(amount, wallet);

        return WithdrawMoney.builder()
                .transactionId( null )
                .walletId(wallet.getWalletId())
                .createdAt(LocalDateTime.now())
                .statusTransaction(processType)
                .amount( amount )
                .previousBalance( wallet.getCurrentBalance())
                .currentBalance( wallet.getCurrentBalance().subtract(amount) )
                .operationType(OperationType.WITHDRAW)
                .build();
    }

    public static TransferMoneySend generateTransferMoneySend(Wallet wallet, Wallet walletTo, BigDecimal amount) {

        StatusTransaction processType = chekProcessTypeTransfer(amount, wallet, walletTo);

        return TransferMoneySend.builder()
                //.transactionId( null )
                .walletId(wallet.getWalletId())
                .createdAt(LocalDateTime.now())
                .statusTransaction(processType)
                .amount( amount )
                .previousBalance( wallet.getCurrentBalance() )
                .currentBalance( wallet.getCurrentBalance().subtract( amount ) )
                .operationType( OperationType.TRANSFER_SEND )
                .build();
    }


    public static TransferMoneyReceived generateTransferMoneyReceived(Wallet walletReceived, BigDecimal amount) {
        Long walletReceivedId = walletReceived.getWalletId();

        return TransferMoneyReceived.builder()
                //.transactionId( null )
                .walletId( walletReceivedId )
                .createdAt(LocalDateTime.now())
                .statusTransaction( StatusTransaction.SUCCESS )
                .amount( amount )
                .previousBalance( walletReceived.getCurrentBalance() )
                .currentBalance( walletReceived.getCurrentBalance().add( amount ) )
                .operationType( OperationType.TRANSFER_RECEIVED )
                .build();
    }


    public static MovementTransaction generateMovementTransaction(Transaction transferSend, Transaction transferReceived) {

        boolean containReceived = !(transferReceived == null || transferReceived.getTransactionId() == null) ;

        return MovementTransaction.builder()
                .transactionId( transferSend.getTransactionId() )
                .walletId( transferSend.getWalletId() )

                .transactionToId( containReceived ? transferReceived.getTransactionId() : null )
                .walletToId( containReceived ? transferReceived.getWalletId() : null )

                .amount( transferSend.getAmount() )
                .createdAt(LocalDateTime.now())

                .operationType(transferSend.getOperationType())

                .build();
    }

    public static DepositSender generateDepositSender(DepositMoney depositMoney, String cpfSender, String senderName, String terminalId) {

        return DepositSender.builder()
                .transactionId(depositMoney.getTransactionId())
                .walletId(depositMoney.getWalletId())
                .cpf(cpfSender)
                .terminalId(terminalId)
                .fullName(senderName)
                .amount(depositMoney.getAmount())
                .build();
    }

//    public static void adjustBalanceWallet(Wallet wallet, BigDecimal previousBalance, BigDecimal currentBalance) {
//        wallet.setPreviousBalance( previousBalance );
//        wallet.setCurrentBalance( currentBalance );
//        wallet.setUpdatedAt( LocalDateTime.now() );
//    }

    public static void adjustBalanceWallet(Wallet wallet, Transaction transaction) {
        wallet.setPreviousBalance( transaction.getPreviousBalance() );
        wallet.setCurrentBalance( transaction.getCurrentBalance() );
        wallet.setUpdatedAt( LocalDateTime.now() );
    }

    public static StatusTransaction chekProcessTypeTransfer(BigDecimal amount, Wallet wallet, Wallet walletTo) {
        StatusTransaction processType = chekProcessTypeGeral(wallet);

        // se as validações gerais foram bem sucessidas, continua para as especificas da transação
        if (processType.equals(StatusTransaction.SUCCESS)) {
            // Valida se a wallet to é diferente da wallet de transferencia
            if (wallet.getWalletId().equals(walletTo.getWalletId())) {
                processType = StatusTransaction.SAME_WALLET;
            }
            // Valida se o valor da transferência é maior do que o saldo existente
            else if (wallet.getCurrentBalance().compareTo( amount ) == CompareBigDecimal.LESS_THAN.getValue() ) {
                processType = StatusTransaction.INSUFFICIENT_BALANCE;
            }
            else if (!walletTo.getCustomer().getStatus().equals(Status.ACTIVE)) {
                processType = StatusTransaction.CUSTOMER_STATUS_INVALID;
            }
            else if (!walletTo.getStatus().equals(Status.ACTIVE)) {
                processType = StatusTransaction.WALLET_STATUS_INVALID;
            }
            else if (amount.compareTo(AMOUNT_MIN_TO_TRANSFER) == CompareBigDecimal.LESS_THAN.getValue()) {
                processType = StatusTransaction.AMOUNT_TRANSFER_INVALID;
            }
        }
        return processType;
    }

    public static StatusTransaction chekProcessTypeDeposit(BigDecimal amount, Wallet wallet) {
        StatusTransaction processType = chekProcessTypeGeral(wallet);

        // se as validações gerais foram bem sucessidas, continua para as especificas da transação
        if (processType.equals(StatusTransaction.SUCCESS)) {
            // VALIDA SE O VALOR DA TRANSAÇÃO É MENOR QUE O MÍNIMO DE DEPOSITO
            if (amount.compareTo(AMOUNT_MIN_TO_DEPOSIT) == CompareBigDecimal.LESS_THAN.getValue()) {
                processType = StatusTransaction.AMOUNT_DEPOSIT_INSUFFICIENT;
            }
        }

        return processType;
    }

    public static StatusTransaction chekProcessTypeWithdraw(BigDecimal amount, Wallet wallet) {
        StatusTransaction processType = chekProcessTypeGeral(wallet);

        // se as validações gerais foram bem sucessidas, continua para as especificas da transação
        if (processType.equals(StatusTransaction.SUCCESS)) {
            // se o valor da transação for menor que mínimo permitido não será criado a transação
            if (wallet.getCurrentBalance().compareTo(amount) == CompareBigDecimal.LESS_THAN.getValue()) {
                processType = StatusTransaction.INSUFFICIENT_BALANCE;
            }
        }

        return processType;
    }

    private static StatusTransaction chekProcessTypeGeral(Wallet wallet) {
        StatusTransaction processType = StatusTransaction.SUCCESS;

        if (wallet == null || wallet.getWalletId() == null) {
            processType = StatusTransaction.WALLET_INVALID;
        }
        else if (wallet.getCustomer() == null || wallet.getCustomer().getCustomerId() == null) {
            processType = StatusTransaction.CUSTOMER_INVALID;
        }
        // verifica se o status dO cliente da wallet é ativa
        else if (!wallet.getCustomer().getStatus().equals(Status.ACTIVE)) {
            processType = StatusTransaction.CUSTOMER_STATUS_INVALID;
        }
        // verifica se o status da wallet é ativa
        else if (!wallet.getStatus().equals(Status.ACTIVE)) {
            processType = StatusTransaction.WALLET_STATUS_INVALID;
        }

        return processType;
    }


}
