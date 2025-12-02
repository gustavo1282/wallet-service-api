package com.guga.walletserviceapi.helpers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.TransferMoneyReceived;
import com.guga.walletserviceapi.model.TransferMoneySend;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.WithdrawMoney;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.model.enums.StatusTransaction;

import net.datafaker.Faker;

public class TransactionUtilsMock {

    private static final boolean APPLY_FILTER_CUSTOMER_BY_STATUS = true;
    private static final int RANGE_CUSTOMER_ID = 1000;
    private static final int TOTAL_CUSTOMER_ID = 1050;
    private static final int LIMIT_LIST_CUSTOMER = 30;

    public static final boolean APPLY_FILTER_WALLET_BY_STATUS = true;
    public static final int RANGE_WALLET_ID = 2000;
    public static final int TOTAL_WALLET_ID = 2150;
    public static final int LIMIT_LIST_WALLET = 150;

    public static final int INI_TRANSACTION_ID = 1;
    public static final int MAX_TRANSACTION_ID = 450;

    public static final int RANGE_TRANSACTION = 11000;
    public static final int RANGE_TRANSFER_MONEY = 11500;
    public static final int RANGE_DEPOSIT_SENDER = 12000;
    public static final int RANGE_MOVEMENT_TRANSACTION = 13000;

    public static final Double MONEY_MIN = 10D;
    public static final Double MONEY_MAX = 800D;

    public static final BigDecimal AMOUNT_MIN_TO_DEPOSIT = new BigDecimal(50);
    public static final BigDecimal AMOUNT_MIN_TO_TRANSFER = new BigDecimal(50);

    private static long SEQUENCE_TRANSACTION;

    public static List<Customer> createCustomerMock() {
        Faker faker = new Faker(new Locale("pt-BR"));
        List<Customer> customers = new ArrayList<>();

        IntStream.range(RANGE_CUSTOMER_ID, TOTAL_CUSTOMER_ID)
            .forEach(i -> {
                String fullName = RandomMock.removeSufixoEPrevixos( faker.name().fullName() ).toUpperCase();
                String[] partName =  fullName.split(" ");
                LocalDate birthDate = defineBirthDateMore18YearOld();
                LocalDateTime dtCreatedAt = RandomMock.generatePastLocalDateTime(2);

                Customer customer = Customer.builder()
                    .customerId((long) (i + 1))
                    .status(defineStatus())
                    .fullName(fullName)
                    .firstName(partName[0])
                    .lastName( partName[partName.length-1] )
                    .birthDate(birthDate)
                    .email(faker.internet().emailAddress( partName[0].concat(".")
                            .concat( partName[partName.length -1 ] ).concat(".")
                            .concat( String.valueOf(birthDate.getMonthValue()) )
                            .concat( String.valueOf(birthDate.getYear()) )
                    ))
                    .phoneNumber(faker.phoneNumber().cellPhone())
                    .documentId(faker.idNumber().valid())
                    .cpf(faker.cpf().valid())
                    .createdAt(dtCreatedAt)
                    .updatedAt(dtCreatedAt)
                    .build();

                    customers.add(customer);

            });
        return customers;
    }


    public static List<Wallet> createWalletMock(List<Customer> customersInput) {
        List<Customer> customersMock = new ArrayList<>(customersInput);

        if (customersMock.isEmpty()) {
            customersMock = createCustomerMock();
        }

        customersMock = findCustomersByStatus(customersMock, Status.ACTIVE);

        customersMock = applyLimiteCustomer(customersMock);

        final List<Customer> customers = customersMock;

        final int totalCustomerItems = customers.size();

        List<Wallet> wallets = new ArrayList<>();

        List<Integer> customersProcess = new ArrayList<>();


        IntStream.range(RANGE_WALLET_ID, TOTAL_WALLET_ID)
           .forEach(i -> {
                
                Customer customer = null;
                boolean isCreateWallet = false;
                int indexCustomer = -1;
                List<Wallet> walletCheck = new ArrayList<>();

                if (customersProcess.size() >= (totalCustomerItems - 1)) {
                    return;
                }

                for(int j = 0; j < 3; j++) {                    
                    indexCustomer = RandomMock.generateNumberByIntervalAndException(0, totalCustomerItems-1, customersProcess);
                    if (indexCustomer >= 0) {
                        customer = customers.get(indexCustomer);                        
                        walletCheck = findWalletActiveByCustomerId(wallets, customer.getCustomerId());
                    }

                    if (walletCheck.isEmpty() && walletCheck.size() == 0) {
                        isCreateWallet = true;
                        break;
                    }
                }

                if (!isCreateWallet || customer == null) {
                    return;
                }

                Long walletId = (long)i;
               
                Status status = defineStatus();

                LocalDateTime createAt = TransactionUtilsMock.defineLocalDAteTimeBetweenYear();

                Wallet wallet = Wallet.builder()
                    .walletId( walletId )
                    .customerId(customer.getCustomerId())
                    .customer(customer)
                    .status( status )
                    .currentBalance( BigDecimal.ZERO )
                    .previousBalance( BigDecimal.ZERO )
                    .createdAt(createAt)
                    .updatedAt(createAt)
                    .loginUser("system")
                    .build();
                
                wallets.add(wallet);

                if (wallet.getStatus().equals(Status.ACTIVE)) {
                    customersProcess.add(indexCustomer);
                }
            }
        );
        return wallets;
    }


    public static Map<String, List<?>> createTransactionMock(List<Wallet> walletInput) {

        Map<String, List<?>> resultsMap = new HashMap<>();

        List<Wallet> walletsMock = new ArrayList<>(walletInput);

        if ( walletsMock.isEmpty() ) {
            walletsMock = createWalletMock(null);
        }

        walletsMock = findWalletsByStatus(walletsMock, Status.ACTIVE);

        walletsMock = applyLimiteWallet(walletsMock);

        final List<Wallet> wallets = new ArrayList<>(walletsMock);

        final int totalWalletItems = wallets.size();

        List<Transaction> transactions = new ArrayList<>();

        List<MovementTransaction> movements = new ArrayList<>();

        List<DepositSender> depositSenders = new ArrayList<>();


        //movementTransactionList = (movementTransactionList == null || movementTransactionList.isEmpty()) ? new ArrayList<>() : movementTransactionList;

        for (int i = INI_TRANSACTION_ID; i < MAX_TRANSACTION_ID; i++) {

            int idxWalletSend = RandomMock.generateIntNumberByInterval(0,  totalWalletItems - 1);
            int idxWalletReceived = RandomMock.generateIntNumberByInterval(0, totalWalletItems - 1);

            final Wallet walletSend = wallets.get(idxWalletSend);
            final Wallet walletReceived = wallets.get(idxWalletReceived);

            OperationType operationType = defineOperationType();

            SEQUENCE_TRANSACTION++;
            BigDecimal amount = generateMoneyBetweenMinAndMaxValue();

            MovementTransaction movement = MovementTransaction.builder().build();;

            switch (operationType) {
                case DEPOSIT:
                    DepositMoney depositMoney = TransactionUtils.generateDepositMoney(walletSend, amount);
                    depositMoney.setTransactionId( RANGE_TRANSACTION + SEQUENCE_TRANSACTION );
                    depositMoney.setLogin(RandomMock.loginFakeMock());
                    if (depositMoney.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {
                        TransactionUtils.setAdjustBalanceWallet(walletSend, depositMoney);

                        movement = TransactionUtils.generateMovementTransaction(depositMoney, null);
                        movement.setMovementId( RANGE_MOVEMENT_TRANSACTION + SEQUENCE_TRANSACTION);
                        movements.add(movement);

                        depositMoney.setMovementId(movement.getMovementId());
                        depositMoney.setMovement(movement);
                    }

                    // Para Mock, gerar dinamicamente o Deposit Sender
                    if ( RandomMock.generateIntNumber (2) == 0 ) {
                        DepositSender depositSender = buildDepositSenderMock(depositMoney);
                        depositSenders.add(depositSender);

                        depositMoney.setDepositSender(depositSender);
                        depositMoney.setDepositSenderId(depositSender.getSenderId());
                    }

                    transactions.add(depositMoney);
                    break;

                case WITHDRAW:
                    WithdrawMoney withdraw = TransactionUtils.generateWithdraw(walletSend, amount );
                    withdraw.setTransactionId( RANGE_TRANSACTION + SEQUENCE_TRANSACTION);
                    withdraw.setLogin(RandomMock.loginFakeMock());
                    if (withdraw.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {
                        TransactionUtils.setAdjustBalanceWallet(walletSend, withdraw);

                        movement = TransactionUtils.generateMovementTransaction(withdraw, null);
                        movement.setMovementId( RANGE_MOVEMENT_TRANSACTION + SEQUENCE_TRANSACTION);
                        movements.add(movement);

                        withdraw.setMovementId(movement.getMovementId());
                        withdraw.setMovement(movement);
                    }

                    transactions.add(withdraw);
                    break;

                case TRANSFER_SEND:
                    TransferMoneySend transferSend = TransactionUtils.generateTransferMoneySend(walletSend, walletReceived, amount);
                    transferSend.setTransactionId( RANGE_TRANSACTION + SEQUENCE_TRANSACTION );
                    transferSend.setLogin(RandomMock.loginFakeMock());

                    if (transferSend.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {
                            
                        SEQUENCE_TRANSACTION++;
                        TransferMoneyReceived transferReceived = TransactionUtils.generateTransferMoneyReceived(walletReceived, amount);
                        transferReceived.setTransactionId( transferSend.getTransactionId() + 1 );
                        transferReceived.setLogin(RandomMock.loginFakeMock());
                        
                        if (transferReceived.getStatusTransaction().equals(StatusTransaction.SUCCESS)){
                            TransactionUtils.setAdjustBalanceWallet(walletSend, transferSend);
                            TransactionUtils.setAdjustBalanceWallet(walletReceived, transferReceived);
                        }
                        else {
                            transferSend.setStatusTransaction(transferReceived.getStatusTransaction());
                        }
                        
                        if (transferSend.getStatusTransaction().equals(StatusTransaction.SUCCESS) &&
                                transferReceived.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {

                            // Gera o movimento de transferencia da transação Destino
                            movement = TransactionUtils.generateMovementTransaction(transferReceived, transferSend);
                            movement.setMovementId( RANGE_MOVEMENT_TRANSACTION + SEQUENCE_TRANSACTION);
                            movements.add(movement);
                            
                            transferReceived.setMovementId(movement.getMovementId());
                            transferReceived.setMovement(movement);

                            transactions.add(transferReceived);
                        }

                        // Gera o movimento de transferencia da transação Envio
                        if (transferSend.getStatusTransaction().equals(StatusTransaction.SUCCESS) &&
                                transferReceived.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {

                            movement = TransactionUtils.generateMovementTransaction(transferSend, transferReceived);
                            movement.setMovementId( RANGE_MOVEMENT_TRANSACTION + SEQUENCE_TRANSACTION);
                            movements.add(movement);

                            transferSend.setMovementId(movement.getMovementId());
                            transferSend.setMovement(movement);
                        }
                    }

                    transactions.add( transferSend );                    
                    break;

                default:
                    break;
            }
        }

        resultsMap.put("transactions", transactions);
        resultsMap.put("movements", movements);
        resultsMap.put("depositSenders", depositSenders);

        return resultsMap;
    }

    public static List<Customer> findCustomersByStatus(List<Customer> customers, Status status) {
        if (!APPLY_FILTER_CUSTOMER_BY_STATUS) {
            return customers;
        }

        return customers.stream()
                .filter(c -> c.getStatus().equals(status))
                .toList();
    }

    public static List<Wallet> findWalletsByStatus(List<Wallet> wallets, Status status) {

        if (!APPLY_FILTER_WALLET_BY_STATUS) {
            return wallets;
        }

        return wallets.stream()
                .filter(c -> c.getStatus().equals(status))
                .toList();
    }


    public static List<Wallet> findWalletActiveByCustomerId(List<Wallet> wallets, Long customerId) {
        return wallets.stream()
                .filter(w -> w.getStatus().equals(Status.ACTIVE)
                        && w.getCustomerId().equals(customerId))
                .toList();
    }


    private static DepositSender buildDepositSenderMock(DepositMoney depositMoney) {
        Faker faker = new Faker(new Locale("pt-BR"));

        return DepositSender.builder()
                .senderId( RANGE_DEPOSIT_SENDER + SEQUENCE_TRANSACTION )
                .fullName( RandomMock.removeSufixoEPrevixos( faker.name().fullName() ).toUpperCase() )
                .terminalId( RandomMock.generateRandomNumbers( 3 ) )
                .amount( depositMoney.getAmount() )
                .cpf( faker.idNumber().valid() )
                .createdAt( LocalDateTime.now() )
                .build();

    }

    public static LocalDate defineBirthDateMore18YearOld() {
        int year = RandomMock.generateIntNumberByInterval(19, 29);
        int month = ThreadLocalRandom.current().nextInt(1, 12 + 1);
        int day = ThreadLocalRandom.current().nextInt(1, 28);
        return RandomMock.getDateNowMinus(year, month, day);
    }

    public static LocalDateTime defineLocalDAteTimeBetweenYear() {
        int year = LocalDateTime.now().getYear();
        int month = ThreadLocalRandom.current().nextInt(1, 12 + 1);
        int day = ThreadLocalRandom.current().nextInt(1, 28);

        return LocalDateTime.now()
                .withYear(year)
                .withMonth(month)
                .withDayOfMonth(day);

    }

    public static Status defineStatus() {
        return generateStatusByWeight();
    }

    public static OperationType defineOperationType() {
        return generateOperationTypeByWeight();
    }

    public static BigDecimal generateMoneyBetweenMinAndMaxValue() {
        int minCents = (int)(MONEY_MIN * 100);
        int maxCents = (int)(MONEY_MAX * 100);

        Random random = new Random();
        int randomCents = random.nextInt(maxCents - minCents) + minCents;

        return new BigDecimal(randomCents).movePointLeft(2);
    }

    private static List<Wallet> applyLimiteWallet(List<Wallet> walletsMock) {
        List<Wallet> list = new ArrayList<>(walletsMock);

        if (list.size() > LIMIT_LIST_WALLET) {
            Collections.shuffle(list);
            list = walletsMock.stream()
                    .limit( LIMIT_LIST_WALLET )
                    .toList();
        }
        return list;
    }

    private static List<Customer> applyLimiteCustomer(List<Customer> customersMock) {
        List<Customer> list = new ArrayList<>(customersMock);

        if (list.size() > LIMIT_LIST_CUSTOMER) {
            Collections.shuffle(list);
            list = list.stream()
                    .limit( LIMIT_LIST_CUSTOMER )
                    .toList();
        }
        return list;
    }

    public static Status generateStatusByWeight() {
        int randomPercentage = RandomMock.generateIntNumber(100);

        if (randomPercentage < 45) {
            return Status.ACTIVE;
        } else if (randomPercentage < 55) {
            return Status.WAITING_VERIFICATION;
        } else if (randomPercentage < 60) {
            return Status.PENDING;
        } else if (randomPercentage < 70) {
            return Status.REVIEW;
        } else if (randomPercentage < 80) {
            return Status.BLOCKED;
        } else if (randomPercentage < 90) {
            return Status.INACTIVE;
        } else {
            return Status.ARCHIVED;
        }
    }

    public static OperationType generateOperationTypeByWeight() {
        int randomPercentage = RandomMock.generateIntNumber(100);

        if (randomPercentage < 90) {
            return OperationType.DEPOSIT;               // 0-45 (45%)
        } else if (randomPercentage < 60) {
            return OperationType.TRANSFER_SEND;         // 46-65 (20%)
        } else if (randomPercentage < 30) {
            return OperationType.WITHDRAW;              // 66-85 (20%)
        }
        else {
            return OperationType.WITHDRAW;
        }
    }

    public static Pageable getDefaultPageable() {
        return PageRequest.of(0, 200,
                Sort.by(Sort.Order.asc("walletId"),
                        Sort.Order.asc("transactionId")
                )
        );
    }

}
