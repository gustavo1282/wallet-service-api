package com.guga.walletserviceapi.helpers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
import com.guga.walletserviceapi.model.dto.TransactionMockResult;
import com.guga.walletserviceapi.model.enums.LoginAuthType;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.model.enums.StatusTransaction;

import net.datafaker.Faker;

public class TransactionUtilsMock {

    private static final boolean APPLY_FILTER_CUSTOMER_BY_STATUS = true;
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

    public static final Double MONEY_MIN = 20D;
    public static final Double MONEY_MAX = 800D;

    public static final BigDecimal AMOUNT_MIN_TO_DEPOSIT = new BigDecimal(50);
    public static final BigDecimal AMOUNT_MIN_TO_TRANSFER = new BigDecimal(50);

    private static long SEQUENCE_TRANSACTION;
    private static long SEQUENCE_LOGIN_AUTH_ID;

    public static List<Customer> createCustomerListMock() {
        List<Customer> customers = new ArrayList<>();

        customers.add(TransactionUtilsMock.addCustomerApplication(RANGE_CUSTOMER_ID + 1));

        int rangeCustomerInit = RANGE_CUSTOMER_ID + 2;
        for (int nextId = rangeCustomerInit; nextId < TOTAL_CUSTOMER_ID; nextId++) {
            customers.add(createCustomerMock(nextId));
        }

        return customers;
    }

    public static List<LoginAuth> createLoginAuthListMock(List<Wallet> wallets) {

        List<LoginAuth> loginAuths = new ArrayList<>();
        String fullnameCustomerApp = GlobalHelper.APP_WALLET_USER.toUpperCase() + " APPLICATION";

        // Pega todos os status possíveis exceto o ACTIVE para a distribuição dos 30%
        List<Status> otherStatuses = java.util.stream.Stream.of(Status.values())
                .filter(s -> s != Status.ACTIVE)
                .toList();

        boolean addLoginAuthApp = true;

        for (Wallet wallet : wallets) {
            Customer customer = wallet.getCustomer();
            // Para cada carteira, decide quantos logins criar (de 1 a 3)
            int loginsToCreate = RandomMock.generateIntNumberByInterval(1, 3);

            for (int i = 0; i < loginsToCreate; i++) {
                // --- Lógica de Distribuição de Status (70% ACTIVE, 30% Outros) ---
                int statusDiceRoll = RandomMock.generateIntNumberByInterval(1, 100);
                Status assignedStatus = (statusDiceRoll <= 70)
                        ? Status.ACTIVE
                        : otherStatuses.get(RandomMock.generateIntNumberByInterval(0, otherStatuses.size() - 1));

                // --- Lógica de Distribuição de Roles (70% USER, 25% ADMIN, 5% Ambos) ---
                int roleDiceRoll = RandomMock.generateIntNumberByInterval(1, 100);
                List<LoginRole> assignedRoles;
                if (roleDiceRoll <= 70) {
                    assignedRoles = Arrays.asList(LoginRole.USER);
                } else if (roleDiceRoll <= 95) { // 71 a 95 (25% de chance)
                    assignedRoles = Arrays.asList(LoginRole.ADMIN);
                } else { // 96 a 100 (5% de chance)
                    assignedRoles = Arrays.asList(LoginRole.USER, LoginRole.ADMIN);
                }

                // --- Geração do Login/AccessKey ---
                LoginAuthType loginAuthType = LoginAuthType.fromValue(RandomMock.generateIntNumberByInterval(0, LoginAuthType.lastIndex()));
                String loginAccess = (customer.getFirstName() + wallet.getWalletId().toString()).toLowerCase();
                
                // Adiciona um sufixo para garantir unicidade, caso haja mais de 1 login por wallet
                if (i > 0) {
                    loginAccess += "_" + i;
                }

                switch (loginAuthType) {
                    case CPF: loginAccess = GlobalHelper.onlyNumbers(customer.getCpf()); break;
                    case E_MAIL: loginAccess = customer.getEmail(); break;
                    default: break;
                }

                String accessKey = GlobalHelper.APP_WALLET_PASS + "_" + wallet.getWalletId().toString() + "_" + i;

                // --- Lógica para o usuário especial 'wallet_user' ---
                // Aplica apenas ao primeiro login da primeira carteira para garantir um usuário previsível
                if (addLoginAuthApp && customer.getFullName().equalsIgnoreCase(fullnameCustomerApp)) {
                    loginAccess = GlobalHelper.APP_WALLET_USER;
                    accessKey = GlobalHelper.APP_WALLET_PASS;
                    assignedRoles = Arrays.asList(LoginRole.ADMIN, LoginRole.USER);
                    addLoginAuthApp = false;
                }

                SEQUENCE_LOGIN_AUTH_ID++;

                loginAuths.add(
                    createLoginAuthMock(
                        (long)(RANGE_LOGIN_AUTH_ID + SEQUENCE_LOGIN_AUTH_ID),
                        customer.getCustomerId(),
                        wallet.getWalletId(),
                        loginAccess,
                        accessKey,
                        loginAuthType,
                        assignedRoles,
                        assignedStatus
                    )

                );
            }
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

    public static Wallet generateNewWallet(Wallet walletInput){
        LocalDateTime createAt = LocalDateTime.now();
        return Wallet.builder()
            .walletId( walletInput.getWalletId() )
            .customerId(walletInput.getCustomerId())
            //.customer(walletInput.getCustomer())
            .status( walletInput.getStatus() )
            .currentBalance( BigDecimal.ZERO )
            .previousBalance( BigDecimal.ZERO )
            .createdAt(createAt)
            .updatedAt(createAt)
            .build();
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
                    .anyMatch(w -> w.getStatus().equals(Status.ACTIVE)));
        
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

    public static TransactionMockResult createTransactionListMock(List<Wallet> walletInput) {

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


        for (int i = INI_TRANSACTION_ID; i < MAX_TRANSACTION_ID; i++) {

            int idxWalletSend = RandomMock.generateIntNumberByInterval(0,  totalWalletItems - 1);
            int idxWalletReceived = RandomMock.generateIntNumberByInterval(0, totalWalletItems - 1);

            final Wallet walletSend = wallets.get(idxWalletSend);
            final Wallet walletReceived = wallets.get(idxWalletReceived);

            OperationType operationType = defineOperationType();

            SEQUENCE_TRANSACTION++;
            BigDecimal amount = generateMoneyBetweenMinAndMaxValue();

            MovementTransaction movement = MovementTransaction.builder().build();

            switch (operationType) {
                case DEPOSIT:
                    DepositMoney depositMoney = TransactionUtils.generateDepositMoney(walletSend, amount, AMOUNT_MIN_TO_DEPOSIT);
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
                    TransferMoneySend transferSend = TransactionUtils.generateTransferMoneySend(walletSend, walletReceived, amount, AMOUNT_MIN_TO_TRANSFER);
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

        return new TransactionMockResult(transactions, movements, depositSenders);
    }

    public static List<Customer> getCustomersByStatus(List<Customer> customers, Status status) {
        if (!APPLY_FILTER_CUSTOMER_BY_STATUS) {
            return customers;
        }

        return customers.stream()
                .filter(c -> c.getStatus().equals(status))
                .toList();
    }

    public static Customer getCustomerApplicationByWallets(List<Wallet> wallets) {
        String fullname = GlobalHelper.APP_WALLET_USER.toUpperCase() + " APPLICATION";

        Wallet result = wallets.stream()        
                .filter(c -> c.getCustomer().getFullName().toUpperCase().equalsIgnoreCase(fullname))
                .findFirst()
                .orElse(null);

        return (result == null || result.getCustomer() == null) ? null : result.getCustomer();
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
            ParamApp.newParam("seq-login-auth-id", "Id Sequencial de LoginAuth  .Id", Long.valueOf("0")),
            ParamApp.newParam("limit_min_to_deposit", "Id Sequencial de LoginAuth  .Id", new BigDecimal(50.00)),
            ParamApp.newParam("limit_min_to_transfer", "Id Sequencial de LoginAuth  .Id", new BigDecimal(50.00))            
        );
        return paramApps;
    }

    public static Customer addCustomerApplication(long nextCustomerId) {
        Customer customer = createCustomerMock(nextCustomerId);
        customer.setFullName( GlobalHelper.APP_WALLET_USER.toUpperCase() + " APPLICATION" );
        customer.setFirstName( GlobalHelper.APP_WALLET_USER.toUpperCase() );
        customer.setLastName( "APPLICATION" );
        customer.setStatus(Status.ACTIVE);
        return customer;        
    }

    public static Customer createCustomerMock(long nextCustomerId) {
        Faker faker = new Faker(new Locale("pt-BR"));

        String fullName = RandomMock.removeSufixoEPrevixos(faker.name().fullName()).toUpperCase();
        String[] partName =  fullName.split(" ");
        LocalDate birthDate = defineBirthDateMore18YearOld();
        LocalDateTime dtCreatedAt = RandomMock.generatePastLocalDateTime(2);
        String cellPhone = faker.phoneNumber().cellPhone();
        String documentId = faker.idNumber().valid();
        String cpf = faker.cpf().valid();
        String email = faker.internet().emailAddress( partName[0].concat(".")
                    .concat( partName[partName.length -1 ] ).concat(".")
                    .concat( String.valueOf(birthDate.getMonthValue()) )
                    .concat( String.valueOf(birthDate.getYear()))
            );

        Customer customer = Customer.builder()
            .customerId(nextCustomerId)
            .status(Status.ACTIVE)
            .fullName(fullName)
            .firstName(partName[0])
            .lastName( partName[partName.length-1] )
            .birthDate(birthDate)
            .email(email)
            .phoneNumber(cellPhone)
            .documentId(documentId)
            .cpf(cpf)
            .createdAt(dtCreatedAt)
            .updatedAt(dtCreatedAt)
            .loginAuthId(null)
            .build();

        return customer;        
    }

    public static DepositMoney generateDepositMoneyMock(List<Transaction> transactions, Wallet walletSend, BigDecimal amount, List<MovementTransaction> movements, List<DepositSender> depositSenders) {

        MovementTransaction movement = MovementTransaction.builder().build();

        DepositMoney depositMoney = TransactionUtils.generateDepositMoney(walletSend, amount, AMOUNT_MIN_TO_DEPOSIT);        
        depositMoney.setTransactionId( nextTransactionId(transactions) );
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

        return depositMoney;
    }

    public static WithdrawMoney generateWithdrawMoneyMock(List<Transaction> transactions, Wallet walletSend, BigDecimal amount, List<MovementTransaction> movements) {
        MovementTransaction movement = MovementTransaction.builder().build();

        WithdrawMoney withdraw = TransactionUtils.generateWithdraw(walletSend, amount );
        withdraw.setTransactionId(nextTransactionId(transactions));
        withdraw.setLogin(RandomMock.loginFakeMock());
        if (withdraw.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {
            TransactionUtils.adjustBalanceWallet(walletSend, withdraw);

            movement = TransactionUtils.generateMovementTransaction(withdraw, null);
            movement.setMovementId( RANGE_MOVEMENT_TRANSACTION + SEQUENCE_TRANSACTION);
            movements.add(movement);

            withdraw.setMovementTransaction(movement);
        }

        return withdraw;
    }

    private static long nextTransactionId(List<Transaction> transactions) {
        return transactions.stream()
            .mapToLong(Transaction::getTransactionId)
            .max()
            .orElse(0L) + 1;
    }

    public static LoginAuth createLoginAuthMock(Long id, Long customerId, Long walletId, String login, String accessKey, LoginAuthType type, List<LoginRole> roles, Status status) {
        LocalDateTime createAt = LocalDateTime.now();
        return LoginAuth.builder()
                .id(id)
                .customerId(customerId)
                .walletId(walletId)
                .login(login)
                .accessKey(accessKey)
                .loginAuthType(type)
                .role(roles)
                .status(status)
                .createdAt(createAt)
                .updatedAt(createAt)
                .build();
    }

    
}
