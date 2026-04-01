package com.guga.walletserviceapi.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.exception.ResourceNotFoundException;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.repository.WalletRepository;
import com.guga.walletserviceapi.service.common.DataPersistenceService;
import com.guga.walletserviceapi.service.common.PersistenceSummary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService implements IWalletApiService {

    private static final Logger LOGGER = LogManager.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final ParamAppService paramAppService;
    private final CustomerService customerService;
    private final DataPersistenceService importService;

    @Transactional(readOnly = true)
    public Wallet getWalletById(Long id) {
        LOGGER.info("WALLET_SERVICE_GET_BY_ID_ENTRY | walletId={}", id);

        Wallet wallet = walletRepository.findById(id).orElse(null);
        if (wallet == null) {
            LOGGER.warn("WALLET_SERVICE_GET_BY_ID_DECISION | walletId={} decision=NOT_FOUND", id);
            throw new ResourceNotFoundException(String.format("Wallet not found with id: %d", id));
        }

        LOGGER.info("WALLET_SERVICE_GET_BY_ID_SUCCESS | walletId={}", wallet.getWalletId());
        return wallet;
    }

    @Transactional(rollbackFor = Exception.class)
    public Wallet saveWallet(Wallet wallet) {
        LOGGER.info("WALLET_SERVICE_SAVE_ENTRY | customerId={}", wallet.getCustomerId());

        Customer customer = customerService.getCustomerById(wallet.getCustomerId());
        LOGGER.info("WALLET_SERVICE_SAVE_AUX_RESULT | customerFound=true customerId={}", customer.getCustomerId());

        Page<Wallet> findAllCustomer = walletRepository.findByCustomerId(wallet.getCustomerId(), GlobalHelper.getDefaultPageable());
        int existingRows = (findAllCustomer == null) ? 0 : findAllCustomer.getNumberOfElements();
        LOGGER.info("WALLET_SERVICE_SAVE_DECISION | customerId={} existingWalletRows={}", wallet.getCustomerId(), existingRows);

        if (findAllCustomer != null && findAllCustomer.hasContent()) {
            LOGGER.warn("WALLET_SERVICE_SAVE_DECISION | customerId={} decision=ALREADY_HAS_WALLET", wallet.getCustomerId());
            throw new ResourceBadRequestException("Customer already has a wallet");
        }

        Long nextWalletId = nextIdGenerate();
        LOGGER.info("WALLET_SERVICE_SAVE_AUX_RESULT | generatedWalletId={}", nextWalletId);
        wallet.setWalletId(nextWalletId);
        wallet.setCustomer(customer);
        wallet.setStatus(Status.PENDING);
        wallet.setCurrentBalance(BigDecimal.ZERO);
        wallet.setPreviousBalance(BigDecimal.ZERO);
        wallet.setLastOperationType(null);
        LocalDateTime currentDT = LocalDateTime.now();
        wallet.setCreatedAt(currentDT);
        wallet.setUpdatedAt(currentDT);
        Wallet newWallet = walletRepository.save(wallet);

        if (newWallet.getWalletId() == null) {
            LOGGER.warn("WALLET_SERVICE_SAVE_DECISION | persistedWalletIdIsNull=true");
            throw new ResourceNotFoundException("Error saving wallet: " + wallet.toString());
        }

        LOGGER.info("WALLET_SERVICE_SAVE_SUCCESS | walletId={} customerId={}", newWallet.getWalletId(), newWallet.getCustomerId());
        return newWallet;
    }

    @Transactional(rollbackFor = Exception.class)
    public Wallet updateWallet(Long walletId, Wallet walletUpdate) {
        LOGGER.info("WALLET_SERVICE_UPDATE_ENTRY | walletId={}", walletId);

        if (walletUpdate == null) {
            LOGGER.warn("WALLET_SERVICE_UPDATE_VALIDATION_FAIL | walletId={} reason=NULL_UPDATE_PAYLOAD", walletId);
            throw new IllegalArgumentException("Wallet update data cannot be null");
        }
        LOGGER.info("WALLET_SERVICE_UPDATE_VALIDATION_OK | walletId={}", walletId);

        Wallet wallet = getWalletById(walletId);

        boolean statusChanged = false;
        boolean currentBalanceChanged = false;
        boolean previousBalanceChanged = false;

        if (walletUpdate.getStatus() != null && !wallet.getStatus().equals(walletUpdate.getStatus())) {
            wallet.setStatus( walletUpdate.getStatus() );
            statusChanged = true;
        }

        if (walletUpdate.getCurrentBalance() != null && wallet.getCurrentBalance().compareTo(walletUpdate.getCurrentBalance()) != 0) {
            wallet.setCurrentBalance( walletUpdate.getCurrentBalance() );
            currentBalanceChanged = true;
        }

        if (walletUpdate.getPreviousBalance() != null && wallet.getPreviousBalance().compareTo(walletUpdate.getPreviousBalance()) != 0) {
            wallet.setPreviousBalance( walletUpdate.getPreviousBalance() );
            previousBalanceChanged = true;
        }

        LOGGER.info(
            "WALLET_SERVICE_UPDATE_DECISION | walletId={} fieldsChanged=status:{},currentBalance:{},previousBalance:{}",
            walletId, statusChanged, currentBalanceChanged, previousBalanceChanged
        );

        wallet.setUpdatedAt(LocalDateTime.now());

        Wallet updatedWallet = walletRepository.save(wallet);
        LOGGER.info("WALLET_SERVICE_UPDATE_SUCCESS | walletId={}", updatedWallet.getWalletId());
        return updatedWallet;
    }

    @Transactional(readOnly = true)
    public Page<Wallet> getAllWallets(Status status, Pageable pageable) {
        LOGGER.info("WALLET_SERVICE_LIST_ENTRY | status={} page={} size={} sort={}",
            status, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()
        );

        Page<Wallet> findResult;
        if (status == null) {
            LOGGER.info("WALLET_SERVICE_LIST_DECISION | branch=findAll");
            findResult = walletRepository.findAll(pageable);
        }
        else {
            LOGGER.info("WALLET_SERVICE_LIST_DECISION | branch=findByStatus");
            findResult = walletRepository.findByStatus(status, pageable);
        }

        if (findResult.isEmpty() || !findResult.hasContent()) {
            LOGGER.warn("WALLET_SERVICE_LIST_DECISION | status={} decision=EMPTY_RESULT", status);
            throw new ResourceNotFoundException("Wallets not found");
        }

        LOGGER.info("WALLET_SERVICE_LIST_SUCCESS | status={} rows={}", status, findResult.getNumberOfElements());
        return findResult;

    }

    public PersistenceSummary importWallets(MultipartFile file) {
        return importService.importJsonFromUpload(file, new TypeReference<List<Wallet>>() {}, walletRepository);
    }

    @Transactional(readOnly = true)
    public Page<Wallet> getWalletByCustomerId(Long customerId, Pageable pageable) {
        LOGGER.info("WALLET_SERVICE_LIST_BY_CUSTOMER_ENTRY | customerId={} page={} size={} sort={}",
            customerId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()
        );

        Page<Wallet> findResult = walletRepository.findByCustomerId(customerId, pageable);

        if (findResult.isEmpty() || !findResult.hasContent()) {
            LOGGER.warn("WALLET_SERVICE_LIST_BY_CUSTOMER_DECISION | customerId={} decision=EMPTY_RESULT", customerId);
            throw new ResourceNotFoundException("Wallets not found by Customer Id");
        }

        LOGGER.info("WALLET_SERVICE_LIST_BY_CUSTOMER_SUCCESS | customerId={} rows={}", customerId, findResult.getNumberOfElements());
        return findResult;
    }

    @Override
    public Long nextIdGenerate() {
        return paramAppService
            .getNextSequenceId(ParamApp.SEQ_WALLET_ID)
            .getValueLong();
    }

}
