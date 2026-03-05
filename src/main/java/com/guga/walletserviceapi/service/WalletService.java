package com.guga.walletserviceapi.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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

    private final WalletRepository walletRepository;
    private final ParamAppService paramAppService;
    private final CustomerService customerService;
    private final DataPersistenceService importService;

    public Wallet getWalletById(Long id) {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Wallet not found with id: %d", id)));
        return wallet;
    }

    public Wallet saveWallet(Wallet wallet) {

        Customer customer = customerService.getCustomerById(wallet.getCustomerId());

        Page<Wallet> findAllCustomer = walletRepository.findByCustomerId(wallet.getCustomerId(), GlobalHelper.getDefaultPageable());

        if (findAllCustomer.isEmpty() || !findAllCustomer.hasContent()) {
            throw new ResourceBadRequestException("Customer already has a wallet");
        }

        wallet.setWalletId(nextIdGenerate());
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
            throw new ResourceNotFoundException("Error saving wallet: " + wallet.toString());
        }

        return newWallet;
    }

    public Wallet updateWallet(Long walletId, Wallet walletUpdate) {
        if (walletUpdate == null) {
            throw new IllegalArgumentException("Wallet update data cannot be null");
        }

        Wallet wallet = getWalletById(walletId);

        if (walletUpdate.getStatus() != null && !wallet.getStatus().equals(walletUpdate.getStatus())) {
            wallet.setStatus( walletUpdate.getStatus() );
        }

        if (walletUpdate.getCurrentBalance() != null && wallet.getCurrentBalance().compareTo(walletUpdate.getCurrentBalance()) != 0) {
            wallet.setCurrentBalance( walletUpdate.getCurrentBalance() );
        }

        if (walletUpdate.getPreviousBalance() != null && wallet.getPreviousBalance().compareTo(walletUpdate.getPreviousBalance()) != 0) {
            wallet.setPreviousBalance( walletUpdate.getPreviousBalance() );
        }

        wallet.setUpdatedAt(LocalDateTime.now());

        return walletRepository.save(wallet);
    }

    public Page<Wallet> getAllWallets(Status status, Pageable pageable) {

        Page<Wallet> findResult;
        if (status == null) {
            findResult = walletRepository.findAll(pageable);
        }
        else {
            findResult = walletRepository.findByStatus(status, pageable);
        }

        if (findResult.isEmpty() || !findResult.hasContent()) {
            throw new ResourceNotFoundException("Wallets not found");
        }

        return findResult;

    }

    public PersistenceSummary importWallets(MultipartFile file) {
        return importService.importJsonFromUpload(file, new TypeReference<List<Wallet>>() {}, walletRepository);
    }

    public Page<Wallet> getWalletByCustomerId(Long customerId, Pageable pageable) {

        Page<Wallet> findResult = walletRepository.findByCustomerId(customerId, pageable);

        if (findResult.isEmpty() || !findResult.hasContent()) {
            throw new ResourceNotFoundException("Wallets not found by Customer Id");
        }

        return findResult;
    }

    @Override
    public Long nextIdGenerate() {
        return paramAppService
            .getNextSequenceId(ParamApp.SEQ_WALLET_ID)
            .getValueLong();
    }

}
