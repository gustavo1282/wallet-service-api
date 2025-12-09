package com.guga.walletserviceapi.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.repository.WalletRepository;

@Service
public class WalletService implements IWalletApiService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ParamAppService paramAppService;

    @Autowired
    private CustomerService customerService;

    public Wallet getWalletById(Long id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Wallet not found with id: %d", id)));
    }

    public Wallet saveWallet(Wallet wallet) {

        Customer customer = customerService.getCustomerById(wallet.getCustomer().getCustomerId());

        Page<Wallet> findAllCustomer = walletRepository.findByCustomerId(wallet.getCustomerId(), GlobalHelper.getDefaultPageable());

        if (findAllCustomer.isEmpty() || !findAllCustomer.hasContent()) {
            throw new ResourceBadRequestException("Customer already has a wallet");
        }

        wallet.setWalletId(nextIdGenerate());
        wallet.setCustomer(customer);
        Wallet newWallet = walletRepository.save(wallet);

        if (newWallet.getWalletId() == null) {
            throw new ResourceNotFoundException("Error saving wallet: " + wallet.toString());
        }

        return newWallet;
    }

    public Wallet updateWallet(Long walletId, Wallet walletUpdate) {

        Wallet wallet = getWalletById(walletId);

        if (!wallet.getStatus().equals(walletUpdate.getStatus())) {
            wallet.setStatus( walletUpdate.getStatus() );
        }

        if (!wallet.getCurrentBalance().equals(walletUpdate.getCurrentBalance())) {
            wallet.setCurrentBalance( walletUpdate.getCurrentBalance() );
        }

        if (!wallet.getPreviousBalance().equals(walletUpdate.getPreviousBalance())) {
            wallet.setPreviousBalance( walletUpdate.getPreviousBalance() );
        }

        wallet.setUpdatedAt(LocalDateTime.now());

        return walletRepository.save(wallet);
    }

    public Page<Wallet> getAllWallets(Pageable pageable) {

        Page<Wallet> findResult = walletRepository.findAll(pageable);

        if (findResult.isEmpty() || !findResult.hasContent()) {
            throw new ResourceNotFoundException("Transactions not found");
        }

        return findResult;

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

            TypeReference<List<Wallet>> walletTypeRef = new TypeReference<List<Wallet>>() { };

            List<Wallet> wallets = mapper.readValue(file.getInputStream(), walletTypeRef);

            for (int i = 0; i < wallets.size(); i += GlobalHelper.BATCH_SIZE) {

                int end = Math.min(wallets.size(), i + GlobalHelper.BATCH_SIZE);
                
                List<Wallet> lote = wallets.subList(i, end);

                walletRepository.saveAll(lote);

            }
        } catch (Exception e) {
            throw new ResourceBadRequestException(e.getMessage());
        }
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
