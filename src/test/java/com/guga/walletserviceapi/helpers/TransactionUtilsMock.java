package com.guga.walletserviceapi.helpers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.TransferMoneyReceived;
import com.guga.walletserviceapi.model.TransferMoneySend;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.WithdrawMoney;
import com.guga.walletserviceapi.model.enums.LoginAuthType;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.model.enums.StatusTransaction;

import net.datafaker.Faker;

public class TransactionUtilsMock {

    private static final boolean APPLY_FILTER_CUSTOMER_BY_STATUS = false;
    private static final int RANGE_CUSTOMER_ID = 1000;
    private static final int TOTAL_CUSTOMER_ID = 1050;
    private static final int LIMIT_LIST_CUSTOMER = 50;
    private static final int RANGE_LOGIN_AUTH_ID = 0;

    public static final boolean APPLY_FILTER_WALLET_BY_STATUS = false;
    public static final int RANGE_WALLET_ID = 2000;
    public static final int TOTAL_WALLET_ID = 2150;
    public static final int LIMIT_LIST_WALLET = 80;

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
    private static long SEQUENCE_LOGIN_AUTH_ID;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public static List<Customer> createCustomerListMock() {

        Faker faker = new Faker(new Locale("pt-BR"));
        

        List<Customer> customers = new ArrayList<>();

        customers.add(TransactionUtilsMock.addCustomerApplication(RANGE_CUSTOMER_ID + 1));

        int rangeCustomerInit = RANGE_CUSTOMER_ID + 2;
        for (int nextId = rangeCustomerInit; nextId < TOTAL_CUSTOMER_ID; nextId++) {
            String fullName = RandomMock.removeSufixoEPrevixos( faker.name().fullName() ).toUpperCase();
            String[] partName =  fullName.split(" ");
            LocalDate birthDate = defineBirthDateMore18YearOld();
            LocalDateTime dtCreatedAt = RandomMock.generatePastLocalDateTime(2);
            String cellPhone = faker.phoneNumber().cellPhone();
            String documentId = faker.idNumber().valid();
            String cpf = faker.cpf().valid();

            Customer customer = Customer.builder()
                .customerId((long) (nextId + 1))
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
                .phoneNumber(cellPhone)
                .documentId(documentId)
                .cpf(cpf)
                .createdAt(dtCreatedAt)
                .updatedAt(dtCreatedAt)
                .loginAuthId(null)
                .build();

            customers.add(customer);
        }

        return customers;
    }

    public static List<LoginAuth> createLoginAuthListMock(List<Wallet> wallets) {

        List<LoginAuth> loginAuths = new ArrayList<>();

        for (Wallet wallet : wallets) {

            Customer customer = wallet.getCustomer();

            Long walletId = wallet.getWalletId();
            
            String loginAccess = customer.getFirstName()
                .concat(wallet.getWalletId().toString())
                .toLowerCase();
            
            String accessKey = "k3Y_".concat(walletId.toString());

            LoginRole loginRole = LoginRole.fromValue(RandomMock.generateIntNumberByInterval(1, 4));
            
            LoginAuthType loginAuthType = LoginAuthType.fromValue(RandomMock.generateIntNumberByInterval(0, 3));
            switch (loginAuthType) {
                case CPF:
                    loginAccess = customer.getCpf();
                    break;
                case E_MAIL:
                    loginAccess = customer.getEmail();
                    break;                    
                default:
                    break;
            }

            // codigo exclusivo para o cliente da aplicação ( wallet_user )
            if (customer.getFirstName().toLowerCase().contains(GlobalHelper.APP_USER_NAME.toLowerCase())) {
                loginAuthType = LoginAuthType.USER_NAME;
                loginAccess = GlobalHelper.APP_USER_NAME + "_" + walletId.toString();
                accessKey = GlobalHelper.APP_PASSWORD + "_" + walletId.toString();
                if (wallet.getStatus().equals(Status.ACTIVE)) {
                    loginAccess = GlobalHelper.APP_USER_NAME;
                    accessKey = GlobalHelper.APP_PASSWORD;
                    loginRole = LoginRole.USER;
                }
            }

            SEQUENCE_LOGIN_AUTH_ID++;

            loginAuths.add(
                LoginAuth.builder()
                    .id( (long)(RANGE_LOGIN_AUTH_ID + SEQUENCE_LOGIN_AUTH_ID) )
                    .status(wallet.getStatus())
                    .customerId(customer.getCustomerId())
                    .walletId(wallet.getWalletId())
                    .login(loginAccess)
                    .accessKey(accessKey)
                    .loginAuthType(loginAuthType)
                    .role(loginRole)
                    .createdAt(customer.getCreatedAt())
                    .updatedAt(customer.getCreatedAt())
                    .build()
            );

        }

        return loginAuths;
    }
    

    public static List<Wallet> createWalletListMock(List<Customer> customersInput) {
        
        List<Customer> customersMock = new ArrayList<>(customersInput);
        
        if (customersMock.isEmpty()) {
            customersMock = createCustomerListMock();
        }

        customersMock = getCustomersByStatus(customersMock, Status.ACTIVE);

        customersMock = applyLimiteCustomer(customersMock);

        final List<Customer> customers = customersMock;

        final int totalCustomerItems = customers.size();

        List<Wallet> wallets = new ArrayList<>();

        Set<Long> processados = new HashSet<>();

        int nextExec = RANGE_WALLET_ID;
        while (nextExec < TOTAL_WALLET_ID) {

            LocalDateTime createAt = TransactionUtilsMock.defineLocalDAteTimeBetweenYear();            

            Long walletId = (long)nextExec;

            Wallet walletNew = validateProcessWallet(customers, wallets, processados);

            if (walletNew == null) {
                break;
            }

            Wallet wallet = Wallet.builder()
                .walletId( walletId )
                .customerId(walletNew.getCustomerId())
                .customer(walletNew.getCustomer())
                .status( walletNew.getStatus() )
                .currentBalance( BigDecimal.ZERO )
                .previousBalance( BigDecimal.ZERO )
                .createdAt(createAt)
                .updatedAt(createAt)
                .build();
            
            wallets.add(wallet);

            nextExec = nextExec + 1;
        }

        return wallets;
    }

    private static Wallet validateProcessWallet( List<Customer> customers, List<Wallet> wallets,
        Set<Long> processados) {

        List<Customer> customersProcess = new ArrayList<>();
        for (Customer c: customers) {
            if (!processados.contains(c.getCustomerId())) {
                customersProcess.add(c);
            }
        }

        Wallet walletOut = null;

        while (customersProcess.size() > 1) {

            int indexCustomer = RandomMock.generateIntNumberByInterval(0, customersProcess.size() - 1);
            
            Customer customer = customersProcess.get(indexCustomer);

            Long customerId = customer.getCustomerId();

            if (processados.contains(customerId)) {
                continue;
            }

            List<Wallet> getWalletsByCustomer = wallets.stream()
                .filter(w -> w.getCustomerId().equals(customerId))
                .toList()
                ;

            boolean hasWalletActive = (!getWalletsByCustomer.isEmpty()) && 
                (getWalletsByCustomer.stream()
                    .anyMatch(w -> w.getStatus() == Status.ACTIVE));
        
            if (hasWalletActive) {
                processados.add(customerId);
                continue;
            }

            Status walletStatus = defineStatus();

            if (!getWalletsByCustomer.isEmpty() && getWalletsByCustomer.size() == 2) {
                walletStatus = Status.ACTIVE;
            }

            if (walletStatus.equals(Status.ACTIVE)) {
                processados.add(customerId);
            }

            walletOut = Wallet.builder()
                .customerId(customerId)
                .customer(customer)
                .status(walletStatus)
                .build();

            break;

        }

        return walletOut;

    }

    public static Map<String, List<?>> createTransactionListMock(List<Wallet> walletInput) {

        Map<String, List<?>> resultsMap = new HashMap<>();

        List<Wallet> walletsMock = new ArrayList<>(walletInput);

        if ( walletsMock.isEmpty() ) {
            walletsMock = createWalletListMock(null);
        }

        walletsMock = getWalletsByStatus(walletsMock, Status.ACTIVE);

        walletsMock = applyLimiteWallet(walletsMock);

        final List<Wallet> wallets = walletsMock;

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
                        TransactionUtils.adjustBalanceWallet(walletSend, depositMoney);

                        movement = TransactionUtils.generateMovementTransaction(depositMoney, null);
                        movement.setMovementId( RANGE_MOVEMENT_TRANSACTION + SEQUENCE_TRANSACTION);
                        movements.add(movement);

                        depositMoney.setMovementTransaction(movement);
                    }

                    // Para Mock, gerar dinamicamente o Deposit Sender
                    if ( RandomMock.generateIntNumber (2) == 0 ) {
                        DepositSender depositSender = generateDepositSenderMock(depositMoney);
                        depositSenders.add(depositSender);

                        depositMoney.setDepositSender(depositSender);
                    }

                    transactions.add(depositMoney);
                    break;

                case WITHDRAW:
                    WithdrawMoney withdraw = TransactionUtils.generateWithdraw(walletSend, amount );
                    withdraw.setTransactionId( RANGE_TRANSACTION + SEQUENCE_TRANSACTION);
                    withdraw.setLogin(RandomMock.loginFakeMock());
                    if (withdraw.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {
                        TransactionUtils.adjustBalanceWallet(walletSend, withdraw);

                        movement = TransactionUtils.generateMovementTransaction(withdraw, null);
                        movement.setMovementId( RANGE_MOVEMENT_TRANSACTION + SEQUENCE_TRANSACTION);
                        movements.add(movement);

                        withdraw.setMovementTransaction(movement);
                    }

                    transactions.add(withdraw);
                    break;

                case TRANSFER_SEND:
                    TransferMoneySend transferSend = TransactionUtils.generateTransferMoneySend(walletSend, walletReceived, amount);
                    transferSend.setTransactionId( RANGE_TRANSACTION + SEQUENCE_TRANSACTION );
                    transferSend.setLogin(RandomMock.loginFakeMock());

                    if (transferSend.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {
                        TransactionUtils.adjustBalanceWallet(walletSend, transferSend);
                            
                        SEQUENCE_TRANSACTION++;
                        TransferMoneyReceived transferReceived = TransactionUtils.generateTransferMoneyReceived(walletReceived, amount);
                        transferReceived.setTransactionId( RANGE_TRANSACTION + SEQUENCE_TRANSACTION );
                        transferReceived.setLogin(RandomMock.loginFakeMock());
                        
                        if (transferReceived.getStatusTransaction().equals(StatusTransaction.SUCCESS)){
                            TransactionUtils.adjustBalanceWallet(walletReceived, transferReceived);
                            
                            // Gera o movimento de transferencia da transação Destino
                            movement = TransactionUtils.generateMovementTransaction(transferReceived, transferSend);
                            movement.setMovementId( RANGE_MOVEMENT_TRANSACTION + SEQUENCE_TRANSACTION);
                            movements.add(movement);
                            
                            transferReceived.setMovementTransaction(movement);

                            transactions.add( transferReceived );
                        } 
                        else {
                            transferSend.setStatusTransaction(transferReceived.getStatusTransaction());
                        }
                        
                        // Gera o movimento de transferencia da transação Envio
                        if (transferSend.getStatusTransaction().equals(StatusTransaction.SUCCESS) &&
                                transferReceived.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {
                            movement = TransactionUtils.generateMovementTransaction(transferSend, transferReceived);
                            movement.setMovementId( RANGE_MOVEMENT_TRANSACTION + SEQUENCE_TRANSACTION);
                            movements.add(movement);

                            transferSend.setMovementTransaction(movement);
                        }

                        transactions.add( transferSend );
                    }
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

    public static List<Customer> getCustomersByStatus(List<Customer> customers, Status status) {
        if (!APPLY_FILTER_CUSTOMER_BY_STATUS) {
            return customers;
        }

        return customers.stream()
                .filter(c -> c.getStatus().equals(status))
                .toList();
    }

    public static List<Wallet> getWalletsByStatus(List<Wallet> wallets, Status status) {

        if (!APPLY_FILTER_WALLET_BY_STATUS) {
            return wallets;
        }

        return wallets.stream()
                .filter(c -> c.getStatus().equals(status))
                .toList();
    }


    private static DepositSender generateDepositSenderMock(DepositMoney depositMoney) {
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

        if (randomPercentage < 60) {
            return Status.ACTIVE;       // 0-59 (60%)
        } else if (randomPercentage < 70) {
            return Status.PENDING;      // 60-69 (10%)
        } else if (randomPercentage < 80) {
            return Status.INACTIVE;     // 70-79 (10%)
        } else if (randomPercentage < 85) {
            return Status.BLOCKED;      // 80-84 (5%)
        } else if (randomPercentage < 90) {
            return Status.REVIEW;       // 85-89 (5%)
        } else if (randomPercentage < 95) {
            return Status.WAITING_VERIFICATION; // 90-94 (5%)
        } else {
            return Status.ARCHIVED;     // 95-99 (5%)
        }
    }

    public static OperationType generateOperationTypeByWeight() {
        int randomPercentage = RandomMock.generateIntNumber(100);

        if (randomPercentage < 50) {
            return OperationType.DEPOSIT;               // 0-45 (45%)
        } else if (randomPercentage < 75) {
            return OperationType.TRANSFER_SEND;         // 46-65 (20%)
        } else if (randomPercentage < 99) {
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

    public static List<ParamApp> createParamsAppMock() {

        List<ParamApp> paramApps = List.of(
            ParamApp.newParam("seq-customer-id", "Id Sequencial de Customer.Id", Long.valueOf("0")),
            ParamApp.newParam("seq-wallet-id", "Id Sequencial de Wallet.Id", Long.valueOf("0")),            
            ParamApp.newParam("seq-transaction-id", "Id Sequencial de Transaction.Id", Long.valueOf("0")),
            ParamApp.newParam("seq-deposit-sender-id", "Id Sequencial de DepositSender.Id", Long.valueOf("0")),
            ParamApp.newParam("seq-movement-id", "Id Sequencial de Movement.Id", Long.valueOf("0")),
            ParamApp.newParam("seq-login-auth-id", "Id Sequencial de LoginAuth  .Id", Long.valueOf("0"))
        );
        return paramApps;
    }

    public static Customer addCustomerApplication(long nextCustomerId) {
        Faker faker = new Faker(new Locale("pt-BR"));

        String fullName = GlobalHelper.APP_USER_NAME.toUpperCase() + " APPLICATION";
        String[] partName =  fullName.split(" ");
        LocalDate birthDate = defineBirthDateMore18YearOld();
        LocalDateTime dtCreatedAt = RandomMock.generatePastLocalDateTime(2);
        String cellPhone = faker.phoneNumber().cellPhone();
        String documentId = faker.idNumber().valid();
        String cpf = faker.cpf().valid();

        Customer customer = Customer.builder()
            .customerId(nextCustomerId)
            .status(Status.ACTIVE)
            .fullName(fullName)
            .firstName(partName[0])
            .lastName( partName[partName.length-1] )
            .birthDate(birthDate)
            .email(faker.internet().emailAddress( partName[0].concat(".")
                    .concat( partName[partName.length -1 ] ).concat(".")
                    .concat( String.valueOf(birthDate.getMonthValue()) )
                    .concat( String.valueOf(birthDate.getYear()) )
            ))
            .phoneNumber(cellPhone)
            .documentId(documentId)
            .cpf(cpf)
            .createdAt(dtCreatedAt)
            .updatedAt(dtCreatedAt)
            .loginAuthId(null)
            .build();

        return customer;        
    }

}
