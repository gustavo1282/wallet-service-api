package com.guga.walletserviceapi.helpers;

import com.guga.walletserviceapi.model.*;
import com.guga.walletserviceapi.model.enums.CompareBigDecimal;
import com.guga.walletserviceapi.model.enums.ProcessTypeTransaction;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.model.enums.TransactionType;
import net.datafaker.Faker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class Helper {

    private static final int RANGE_CUSTOMER_ID = 1000;
    private static final int TOTAL_CUSTOMER_ID = 1100;

    private static final int RANGE_WALLET_ID = 2000;
    private static final int TOTAL_WALLET_ID = 2100;

    private static final int RANGE_TRANSACTION_ID = 4000;
    private static final int TOTAL_TRANSACTION_ID = 4200;

    private static final int RANGE_TRANSFER_MONEY = 5000;

    private static final int RANGE_TRANSFER_MONEY_TO = 6000;

    private static final int LIMIT_LIST_CUSTOMER = 35;

    private static final Double MONEY_MIN = 10D;
    private static final Double MONEY_MAX = 800D;

    private static final BigDecimal AMOUNT_MIN_TO_DEPOSIT = new BigDecimal(50);

    private static final BigDecimal AMOUNT_MIN_TO_TRANSFER = new BigDecimal(50);

    public static List<Customer> createCustomerListMock() {
        Faker faker = new Faker(new Locale("pt-BR"));
        List<Customer> customers = new ArrayList<>();

        IntStream.range(RANGE_CUSTOMER_ID, TOTAL_CUSTOMER_ID)
            .forEach(i -> {
                String fullName = RandomGenerator.removeSufixoEPrevixos( faker.name().fullName() ).toUpperCase();
                String[] partName =  fullName.split(" ");
                LocalDate birthDate = defineBirthDateMore18YearOld();
                LocalDateTime dtCreatedAt = RandomGenerator.generatePastLocalDateTime(2);

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

    public static List<Wallet> createWalletListMock(List<Customer> customersInput) {
        List<Customer> customersMock = new ArrayList<>(customersInput);

        if (customersMock.isEmpty()) {
            customersMock = createCustomerListMock();
        }

        customersMock = getCustomersByStatus(customersMock, Status.ACTIVE);

        if (customersMock.size() > LIMIT_LIST_CUSTOMER) {
            customersMock = customersMock.stream()
                    .limit( LIMIT_LIST_CUSTOMER )
                    .toList();
        }

        final List<Customer> customers = customersMock;

        final int totalCustomerItems = customers.size();

        List<Wallet> wallets = new ArrayList<>();

        IntStream.range(RANGE_WALLET_ID, TOTAL_WALLET_ID)
           .forEach(i -> {
                int indexCustomer = RandomGenerator.generateIntNumberByInterval(0, totalCustomerItems - 1);
                Customer customer = customers.get(indexCustomer);
                LocalDateTime createAt = Helper.defineLocalDAteTimeBetweenYear();

                Long walletId = RANGE_WALLET_ID + (long)i;

                Wallet wallet = Wallet.builder()
                        .walletId( walletId )
                        .customerId(customer.getCustomerId())
                        .customer(customer)
                        .status(defineStatus())
                        .currentBalance( BigDecimal.ZERO )
                        .previousBalance( BigDecimal.ZERO )
                        .createdAt(createAt)
                        .updatedAt(createAt)
                        .build();

                wallets.add(wallet);
           }
           );

        customersInput = customers;
        return wallets;
    }

    public static List<Customer> getCustomersByStatus(List<Customer> customers, Status status) {
        return customers.stream()
                .filter(c -> c.getStatus().equals(status))
                .toList();
    }

    public static List<Wallet> getWalletsByStatus(List<Wallet> wallets, Status status) {
        return wallets.stream()
                .filter(c -> c.getStatus().equals(status))
                .toList();
    }

    public static List<Transaction> createTransactionListMock(List<Wallet> walletInput) {
        List<Wallet> walletsMock = new ArrayList<Wallet>(walletInput);

        if ( walletsMock.isEmpty() ) {
            walletsMock = createWalletListMock(null);
        }

        walletsMock = getWalletsByStatus(walletsMock, Status.ACTIVE);

        final List<Wallet> wallets = walletsMock;

        final int totalWalletItems = wallets.size();

        List<Transaction> transactions = new ArrayList<>();

        IntStream.range(RANGE_TRANSACTION_ID, TOTAL_TRANSACTION_ID)
            .forEach(i -> {

                Long transactionId = RANGE_TRANSACTION_ID + (long)i;

                int idxWallet = RandomGenerator.generateIntNumberByInterval(0,  totalWalletItems - 1);
                int idxWalletTo = RandomGenerator.generateIntNumberByInterval(0, totalWalletItems - 1);

                final Wallet wallet = wallets.get(idxWallet);
                final Wallet walletTo = wallets.get(idxWalletTo);

                TransactionType transactionType = defineTransactionType();
                Transaction transaction = null;
                switch (transactionType) {
                    case WITHDRAW:
                        transaction = insertTransactionWithdraw(transactionId, wallet);
                        break;
                    case DEPOSIT:
                        transaction = insertTransactionDeposit(transactionId, wallet);
                        break;
                    case TRANSFER_TO:
                        transaction = insertTransactionTransfer(transactionId, wallet, walletTo);
                        break;
                }

                if (transaction != null) {
                    if (transaction.getProcessTypeTransaction().equals(ProcessTypeTransaction.SUCCESS)) {
                        wallet.setPreviousBalance( transaction.getPreviousBalance() );
                        wallet.setCurrentBalance( transaction.getCurrentBalance() );
                        wallet.setUpdatedAt(LocalDateTime.now());

                        // Atualiza dados da wallet de recebimento da transferencia (WalletTo)
                        if (transaction.getTransactionType().equals(TransactionType.TRANSFER_TO)) {
                            walletTo.setPreviousBalance( walletTo.getCurrentBalance() );
                            walletTo.setCurrentBalance( walletTo.getCurrentBalance().add( transaction.getAmount() ) );
                            walletTo.setUpdatedAt(LocalDateTime.now());
                        }
                    }
                    transactions.add(transaction);
                }
            });
        return transactions;
    }

    private static TransferMoney insertTransactionTransfer(Long transactionId, Wallet wallet, Wallet walletTo) {

        ProcessTypeTransaction processType = ProcessTypeTransaction.SUCCESS;

        BigDecimal amount = generateMoneyBetweenMinAndMaxValue(MONEY_MIN, MONEY_MAX);

        // Valida se a wallet to é diferente da wallet de transferencia
        if (wallet.getWalletId().equals(walletTo.getWalletId())) {
            processType = ProcessTypeTransaction.SAME_WALLET;
        }
        // Valida se o valor da transferência é maior do que o saldo existente
        else if (wallet.getCurrentBalance().compareTo( amount ) == CompareBigDecimal.LESS_THAN.getValue() ) {
            processType = ProcessTypeTransaction.INSUFFICIENT_BALANCE;
        }
        else if (!walletTo.getStatus().equals(Status.ACTIVE)) {
            processType = ProcessTypeTransaction.WALLET_STATUS_INVALID;
        }
        else if (amount.compareTo(AMOUNT_MIN_TO_TRANSFER) == CompareBigDecimal.LESS_THAN.getValue()) {
            processType = ProcessTypeTransaction.AMOUNT_TRANSFER_INVALID;
        }

        Long idTransferMoneyTo = RANGE_TRANSFER_MONEY - (transactionId - RANGE_TRANSACTION_ID) ;

        TransferMoneyTo transferMoneyTo = TransferMoneyTo.builder()
                .idTransferMoneyTo( idTransferMoneyTo )
                .transactionId(transactionId)
                .walletId( walletTo.getWalletId() )
                .wallet( walletTo )
                .amount( amount )
                .createdAt(LocalDateTime.now())
                .build();

        return TransferMoney.builder()
            .transactionId(transactionId)
            .walletId(wallet.getWalletId())
            .wallet(wallet)
            .createdAt(LocalDateTime.now())

            .processTypeTransaction(processType)

            .amount( amount )
            .previousBalance( wallet.getCurrentBalance() )
            .currentBalance( wallet.getCurrentBalance().subtract( amount ) )

            .idTransferMoneyTo(transferMoneyTo.getIdTransferMoneyTo())
            .transferMoneyTo( transferMoneyTo )
            .transactionType(TransactionType.TRANSFER_TO)

            .build();
    }

    private static DepositMoney insertTransactionDeposit(Long transactionId, Wallet wallet) {

        ProcessTypeTransaction processType = ProcessTypeTransaction.SUCCESS;

        BigDecimal amount = generateMoneyBetweenMinAndMaxValue(MONEY_MIN, MONEY_MAX);
        if ( amount.compareTo(AMOUNT_MIN_TO_DEPOSIT) == CompareBigDecimal.LESS_THAN.getValue() ) {
            processType = ProcessTypeTransaction.AMOUNT_DEPOSIT_INSUFFICIENT;
        }

        return DepositMoney.builder()
                .transactionId(transactionId)
                .walletId(wallet.getWalletId())
                .wallet(wallet)
                .createdAt(LocalDateTime.now())

                .processTypeTransaction( processType )

                .amount( amount )
                .previousBalance( wallet.getCurrentBalance( ))
                .currentBalance( wallet.getCurrentBalance().add(amount) )

                .transactionType(TransactionType.DEPOSIT)
                .build()
                ;
    }


    private static WithdrawMoney insertTransactionWithdraw(Long transactionId, Wallet wallet) {

        BigDecimal amount = generateMoneyBetweenMinAndMaxValue(MONEY_MIN, MONEY_MAX);

        ProcessTypeTransaction processType = ProcessTypeTransaction.SUCCESS;

        // se o valor da transação for menor que mínimo permitido não será criado a transação
        if (wallet.getCurrentBalance().compareTo(amount) == CompareBigDecimal.LESS_THAN.getValue()) {
            processType = ProcessTypeTransaction.INSUFFICIENT_BALANCE;
        }

        return WithdrawMoney.builder()
               .transactionId(transactionId)
               .walletId(wallet.getWalletId())
               .wallet(wallet)
               .createdAt(LocalDateTime.now())
               .processTypeTransaction(processType)

               .amount( amount )
               .previousBalance( wallet.getCurrentBalance())
               .currentBalance( wallet.getCurrentBalance().subtract(amount) )

               .transactionType(TransactionType.WITHDRAW)
               .build()
               ;
    }

    private static TransactionType defineTransactionType() {
        int id = RandomGenerator.generateIntNumberByInterval(1, TransactionType.values().length + 1);
        return TransactionType.fromValue(id);
    }


    public static LocalDate defineBirthDateMore18YearOld() {
        int year = RandomGenerator.generateIntNumberByInterval(19, 29);
        int month = ThreadLocalRandom.current().nextInt(1, 12 + 1);
        int day = ThreadLocalRandom.current().nextInt(1, 28);
        return RandomGenerator.getDateNowMinus(year, month, day);
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
        return Status.fromValue(RandomGenerator
                .generateIntNumberByInterval(1, Status.values().length + 1)
            );
    }

    public static BigDecimal generateMoneyBetweenMinAndMaxValue(double min, double max) {
        int minCents = (int)(min * 100);
        int maxCents = (int)(max * 100);

        Random random = new Random();
        int randomCents = random.nextInt(maxCents - minCents) + minCents;

        return new BigDecimal(randomCents).movePointLeft(2);
    }

}
