package com.guga.walletserviceapi.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.exception.ResourceNotFoundException;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.helpers.TransactionUtils;
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
import com.guga.walletserviceapi.repository.DepositSenderRepository;
import com.guga.walletserviceapi.repository.MovementTransferRepository;
import com.guga.walletserviceapi.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DepositSenderRepository depositSenderRepository;

    @Autowired
    private MovementTransferRepository movementTransferRepository;

    @Autowired
    private WalletService walletService;

    public static final BigDecimal AMOUNT_MIN_TO_DEPOSIT = new BigDecimal(50);
    public static final BigDecimal AMOUNT_MIN_TO_TRANSFER = new BigDecimal(50);

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + String.valueOf(id)));
    }

    public Page<Transaction> getTransactionByWalletId(Long id, Pageable pageable) {
        
        Optional<Page<Transaction>> findResult = transactionRepository.findByWallet_WalletId(id, pageable);

        if (findResult.isEmpty() || !findResult.get().hasContent()) {
            throw new ResourceNotFoundException("Transaction not found by wallet id: " + String.valueOf(id));
        }

        return findResult.get();

    }

    public DepositMoney saveDepositMoney(Long walletId, BigDecimal amount, String cpfSender,
                                         String terminalId, String senderName)
    {
        Wallet wallet = walletService.getWalletById(walletId);

        DepositMoney depositMoney = TransactionUtils.generateDepositMoney(wallet, amount);

        DepositMoney depositMoneySaved = transactionRepository.save(depositMoney);

        if (!depositMoney.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {
            throw new ResourceBadRequestException("Invalid Business Rules - "
                    .concat(depositMoney.getStatusTransaction().name())
            );
        }

        TransactionUtils.setAdjustBalanceWallet(wallet, depositMoney);
        walletService.updateWallet(wallet.getWalletId(), wallet);

        MovementTransaction movement = TransactionUtils
                .generateMovementTransaction(depositMoneySaved, null);
        MovementTransaction movementSaved = movementTransferRepository.save(movement);
        depositMoney.setMovement(movementSaved);

        if ( senderName != null && senderName.length() > 5 &&
                cpfSender != null && cpfSender.length() > 5 ) {

            DepositSender depositSender = TransactionUtils.generateDepositSender(depositMoney,
                    cpfSender, senderName, terminalId);

            DepositSender depositSenderSaved = depositSenderRepository.save(depositSender);

            depositMoney.setDepositSender(depositSenderSaved);

        }

        return depositMoneySaved;
    }

    /***
     * Salvar uma transação de Saque
     * Regra,
     *     1 - receber o walledId e o valor de saque
     *     2 - vallidar o walletId
     *     3 - gerar o objeto de saque (withdraw) - generateWithdraw. neste processo é validado as business rules
     *     4 - registrar o saque na tabela transactions
     *     5 - gerar transacao confirmada, atualiza o valor de saldo da wallet
     *     6 - registra o movimento da transação
     * @param walletId
     * @param amount
     * @return
     */
    public WithdrawMoney saveWithdrawMoney(Long walletId, BigDecimal amount) {

        Wallet wallet = walletService.getWalletById(walletId);
        if (wallet == null || wallet.getWalletId() == null) {
            throw new ResourceBadRequestException("The transaction does not contain a valid wallet");
        }

        WithdrawMoney withdraw = TransactionUtils.generateWithdraw(wallet, amount);

        WithdrawMoney withdrawSaved = transactionRepository.save(withdraw);

        if (!withdraw.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {
            throw new ResourceBadRequestException("Invalid Business Rules - "
                    .concat(withdraw.getStatusTransaction().name())
            );
        }

        if (withdrawSaved.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {

            TransactionUtils.setAdjustBalanceWallet(wallet, withdrawSaved);
            walletService.updateWallet( walletId, wallet );

            MovementTransaction movement = TransactionUtils
                    .generateMovementTransaction(withdrawSaved, null);
            MovementTransaction movementSaved = movementTransferRepository.save(movement);
            withdrawSaved.setMovement(movementSaved);

        }

        return withdrawSaved;
    }

    public TransferMoneySend saveTransferMoneySend(Long walletIdSend, Long walletIdReceived, BigDecimal amount) {

        Wallet walletSend = walletService.getWalletById(walletIdSend);

        Wallet walletReceived = walletService.getWalletById(walletIdReceived);

        TransferMoneySend transferSend = TransactionUtils.generateTransferMoneySend(walletSend,
                walletReceived, amount);

        TransferMoneyReceived transferReceived = TransferMoneyReceived.builder()
                .statusTransaction(StatusTransaction.INVALID)
                .build();

        TransferMoneySend transferSendSaved = transactionRepository.save(transferSend);

        if (!transferSend.getStatusTransaction().equals(StatusTransaction.SUCCESS) ||
                !transferReceived.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {
            throw new ResourceBadRequestException("Invalid Business Rules - "
                    .concat(transferSend.getStatusTransaction().name())
            );
        }

        if (transferSendSaved.getStatusTransaction().equals(StatusTransaction.SUCCESS)) {

            TransactionUtils.setAdjustBalanceWallet(walletSend, transferSend);
            walletService.updateWallet(walletSend.getWalletId(), walletSend);

            MovementTransaction movementTransactionSend = TransactionUtils.generateMovementTransaction(transferSend, transferReceived);
            movementTransferRepository.save(movementTransactionSend);
            transferSendSaved.setMovement(movementTransactionSend);


            transferReceived = TransactionUtils.generateTransferMoneyReceived(walletReceived, amount);

            if (transferReceived.getStatusTransaction().equals(StatusTransaction.SUCCESS)){

                TransactionUtils.setAdjustBalanceWallet(walletReceived, transferReceived);
                walletService.updateWallet(walletReceived.getWalletId(), walletReceived);

                MovementTransaction movementReceived = TransactionUtils.generateMovementTransaction(transferReceived, transferSend);
                MovementTransaction movementReceivedSaved =  movementTransferRepository.save(movementReceived);
                transferReceived.setMovement(movementReceivedSaved);
            }

        }

        return transferSendSaved;
    }

    public Page<Transaction> filterTransactionByWalletIdAndProcessType(Long walletId, StatusTransaction typeTransaction,
                                                                       Pageable pageable) {
        List<Specification<Transaction>> specs = new ArrayList<>();

        // FILTRO 1: byWalletId (Long)
        if (walletId != null) {
            specs.add((root, query, builder) ->
                    builder.equal(root.get("walletId"), walletId));
        }

        // FILTRO 2: byStatus (Enum)
        if (typeTransaction != null) {
            specs.add((root, query, builder) ->
                    builder.equal(root.get("transactionType"), typeTransaction)); // ou 'status' dependendo do nome do campo
        }

        // 3. Combina todas as especificações com 'AND'
        Specification<Transaction> combinedSpec = Specification.where(null); // Inicia com Specification.where(null)

        for (Specification<Transaction> spec : specs) {
            // Usa Specification.and() para combinar todos os filtros
            combinedSpec = combinedSpec.and(spec);
        }

        // 4. Executa a consulta paginada
        return transactionRepository.findAll(combinedSpec, pageable);

    }

    public static MovementTransaction generateTransferMoney(Transaction transferSend, Transaction transferReceived) {

        boolean containReceived = !(transferReceived == null || transferReceived.getTransactionId() == null) ;

        return MovementTransaction.builder()
                .movementId( null )

                .transactionId( transferSend.getTransactionId() )
                .walletId( transferSend.getWallet().getWalletId() )

                .transactionReferenceId( containReceived ? transferReceived.getTransactionId() : null )
                .walletReferenceId( containReceived ? transferReceived.getWallet().getWalletId() : null)

                .amount( transferSend.getAmount() )
                .createdAt(LocalDateTime.now())
                .build();
    }

    public DepositMoney generateDepositMoney(Wallet wallet, BigDecimal amount) {
        StatusTransaction processType = checkProcessTypeDeposit(wallet, amount);

        return DepositMoney.builder()
                //.transactionId( null )
                .wallet(wallet)
                //.walletId(wallet.getWalletId())
                .createdAt(LocalDateTime.now())
                .statusTransaction( processType )
                .amount( amount )
                .previousBalance( wallet.getCurrentBalance( ))
                .currentBalance( wallet.getCurrentBalance().add(amount) )
                .operationType(OperationType.DEPOSIT)
                .build();
    }

    private StatusTransaction checkProcessTypeDeposit(Wallet wallet, BigDecimal amount) {
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

    private StatusTransaction chekProcessTypeGeral(Wallet wallet) {
        StatusTransaction processType = StatusTransaction.SUCCESS;

        // verifica se o status dO cliente da wallet é ativa
        if (!wallet.getCustomer().getStatus().equals(Status.ACTIVE)) {
            processType = StatusTransaction.CUSTOMER_STATUS_INVALID;
        }
        // verifica se o status da wallet é ativa
        else if (!wallet.getStatus().equals(Status.ACTIVE)) {
            processType = StatusTransaction.WALLET_STATUS_INVALID;
        }

        return processType;
    }

    /***
     * Função responsável por realizar carga inicial de dados para a tabela correspondente a partir de um 
     * arquivo no formato JSON
     * @param file
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadCsvAndSave(MultipartFile file) throws Exception {
        try {
            ObjectMapper mapper = FileUtils.instanceObjectMapper();

            TypeReference<List<Transaction>> transactionTypeRef = new TypeReference<List<Transaction>>() { };

            List<Transaction> transactions = mapper.readValue(file.getInputStream(), transactionTypeRef);

            for (int i = 0; i < transactions.size(); i += GlobalHelper.BATCH_SIZE) {

                int end = Math.min(transactions.size(), i + GlobalHelper.BATCH_SIZE);
                
                List<Transaction> lote = transactions.subList(i, end);

                transactionRepository.saveAll(lote);

            }
        } catch (Exception e) {
            throw new ResourceBadRequestException(e.getMessage());
        }
    }

}
