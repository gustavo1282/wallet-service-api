package com.guga.walletserviceapi.helpers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.TransferMoneyReceived;
import com.guga.walletserviceapi.model.TransferMoneySend;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.WithdrawMoney;
import com.guga.walletserviceapi.model.enums.CompareBigDecimal;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.model.enums.StatusTransaction;

public class TransactionUtils {

    public static BigDecimal AMOUNT_MIN_TO_DEPOSIT = new BigDecimal(50);
    public static BigDecimal AMOUNT_MIN_TO_TRANSFER = new BigDecimal(50);

    public static DepositMoney generateDepositMoney(Wallet wallet, BigDecimal amount) {
        StatusTransaction statusTransaction = chekProcessTypeDeposit(wallet, amount);

        return DepositMoney.builder()
                .walletId(wallet.getWalletId())
                .wallet(wallet)
                .createdAt(LocalDateTime.now())
                .statusTransaction( statusTransaction )
                .amount( amount )
                .previousBalance( wallet.getCurrentBalance( ))
                .currentBalance( wallet.getCurrentBalance().add(amount) )
                .operationType(OperationType.DEPOSIT)
                .depositSenderId(null)
                .build();
    }

    public static WithdrawMoney generateWithdraw(Wallet wallet, BigDecimal amount) {
        StatusTransaction sttType = chekProcessTypeWithdraw(wallet, amount);

        return WithdrawMoney.builder()
                .transactionId( null )
                .walletId(wallet.getWalletId())
                .wallet(wallet)
                .createdAt(LocalDateTime.now())
                .statusTransaction(sttType)
                .amount( amount )
                .previousBalance( wallet.getCurrentBalance())
                .currentBalance( wallet.getCurrentBalance().subtract(amount) )
                .operationType(OperationType.WITHDRAW)
                .build();
    }

    public static TransferMoneySend generateTransferMoneySend(Wallet wallet, Wallet walletTo, BigDecimal amount) {
        StatusTransaction sttType = chekProcessTypeTransfer(wallet, walletTo, amount);

        return TransferMoneySend.builder()
                //.transactionId( null )
                .walletId(wallet.getWalletId())
                .wallet(wallet)
                .createdAt(LocalDateTime.now())
                .statusTransaction(sttType)
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
                .wallet(walletReceived)
                .createdAt(LocalDateTime.now())
                .statusTransaction( StatusTransaction.SUCCESS )
                .amount( amount )
                .previousBalance( walletReceived.getCurrentBalance() )
                .currentBalance( walletReceived.getCurrentBalance().add( amount ) )
                .operationType( OperationType.TRANSFER_RECEIVED )
                .build();
    }


    public static MovementTransaction generateMovementTransaction(Transaction transferSend, Transaction transferReceived) {

        boolean containReceived = (transferReceived != null && transferReceived.getTransactionId() != null) ;

        return MovementTransaction.builder()
                .transactionId( transferSend.getTransactionId() )
                .walletId( transferSend.getWalletId() )

                .transactionReferenceId( containReceived ? transferReceived.getTransactionId() : null )
                .walletReferenceId( containReceived ? transferReceived.getWalletId() : null )

                .amount( transferSend.getAmount() )
                .createdAt(LocalDateTime.now())

                .operationType(transferSend.getOperationType())
                .statusTransaction(transferSend.getStatusTransaction())

                .build();
    }

    public static DepositSender generateDepositSender(DepositMoney depositMoney, String cpfSender, String senderName, String terminalId) {

        return DepositSender.builder()
                //.transaction(depositMoney)
                .cpf(cpfSender)
                .terminalId(terminalId)
                .fullName(senderName)
                .amount(depositMoney.getAmount())
                .build();
    }

    public static void setAdjustBalanceWallet(Wallet wallet, Transaction transaction) {
        wallet.setPreviousBalance(transaction.getPreviousBalance() );
        wallet.setCurrentBalance(transaction.getCurrentBalance() );
        wallet.setLastOperationType(transaction.getOperationType());
        wallet.setUpdatedAt( LocalDateTime.now() );
    }

    public static StatusTransaction chekProcessTypeTransfer(Wallet wallet, Wallet walletTo, BigDecimal amount) {
        StatusTransaction sttType = chekProcessTypeGeral(wallet);

        // se as validações gerais foram bem sucessidas, continua para as especificas da transação
        if (sttType.equals(StatusTransaction.SUCCESS)) {
            // Valida se a wallet to é diferente da wallet de transferencia
            if (wallet.getWalletId().equals(walletTo.getWalletId())) {
                sttType = StatusTransaction.SAME_WALLET;
            }
            // Valida se o valor da transferência é maior do que o saldo existente
            else if (wallet.getCurrentBalance().compareTo( amount ) == CompareBigDecimal.LESS_THAN.getValue() ) {
                sttType = StatusTransaction.INSUFFICIENT_BALANCE;
            }
            else if (!walletTo.getCustomer().getStatus().equals(Status.ACTIVE)) {
                sttType = StatusTransaction.CUSTOMER_STATUS_INVALID;
            }
            else if (!walletTo.getStatus().equals(Status.ACTIVE)) {
                sttType = StatusTransaction.WALLET_STATUS_INVALID;
            }
            else if (amount.compareTo(AMOUNT_MIN_TO_TRANSFER) == CompareBigDecimal.LESS_THAN.getValue()) {
                sttType = StatusTransaction.AMOUNT_TRANSFER_INVALID;
            }
        }
        return sttType;
    }

    public static StatusTransaction chekProcessTypeDeposit(Wallet wallet, BigDecimal amount) {
        StatusTransaction sttType = chekProcessTypeGeral(wallet);

        // se as validações gerais foram bem sucessidas, continua para as especificas da transação
        if (sttType.equals(StatusTransaction.SUCCESS)) {
            // VALIDA SE O VALOR DA TRANSAÇÃO É MENOR QUE O MÍNIMO DE DEPOSITO
            if (amount.compareTo(AMOUNT_MIN_TO_DEPOSIT) == CompareBigDecimal.LESS_THAN.getValue()) {
                sttType = StatusTransaction.AMOUNT_DEPOSIT_INSUFFICIENT;
            }
        }
        return sttType;
    }

    public static StatusTransaction chekProcessTypeWithdraw(Wallet wallet, BigDecimal amount) {
        StatusTransaction sttType = chekProcessTypeGeral(wallet);

        // se as validações gerais foram bem sucessidas, continua para as especificas da transação
        if (sttType.equals(StatusTransaction.SUCCESS)) {
            // se o valor da transação for menor que mínimo permitido não será criado a transação
            if (wallet.getCurrentBalance().compareTo(amount) == CompareBigDecimal.LESS_THAN.getValue()) {
                sttType = StatusTransaction.INSUFFICIENT_BALANCE;
            }
        }
        return sttType;
    }

    private static StatusTransaction chekProcessTypeGeral(Wallet wallet) {
        StatusTransaction sttType = StatusTransaction.SUCCESS;

        if (wallet == null || wallet.getWalletId() == null) {
            sttType = StatusTransaction.WALLET_INVALID;
        }
        else if (wallet.getCustomer() == null || wallet.getCustomer().getCustomerId() == null) {
            sttType = StatusTransaction.CUSTOMER_INVALID;
        }
        // verifica se o status dO cliente da wallet é ativa
        else if (!wallet.getCustomer().getStatus().equals(Status.ACTIVE)) {
            sttType = StatusTransaction.CUSTOMER_STATUS_INVALID;
        }
        // verifica se o status da wallet é ativa
        else if (!wallet.getStatus().equals(Status.ACTIVE)) {
            sttType = StatusTransaction.WALLET_STATUS_INVALID;
        }

        return sttType;
    }


}
