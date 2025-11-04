package com.guga.walletserviceapi.service;

import com.guga.walletserviceapi.config.ResourceBadRequestException;
import com.guga.walletserviceapi.config.ResourceNotFoundException;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CustomerService customerService;

    public Wallet getWalletById(Long id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Wallet not found with id: %d", id)));
    }

    public Wallet saveWallet(Wallet wallet) {

        Customer customer = customerService.getCustomerById(wallet.getCustomerId());

        List<Wallet> existingWallet = walletRepository.findAllWalletsByCustomerId(wallet.getCustomerId())
                .orElse(null);
        if (existingWallet != null && existingWallet.isEmpty()) {
            throw new ResourceBadRequestException("Customer already has a wallet");
        }

        wallet.setCustomer(customer);

        Wallet newWallet = walletRepository.save(wallet);

        if (newWallet.getWalletId() == null) {
            throw new ResourceNotFoundException("Error saving wallet");
        }

        // newWallet.setCustomer(customer);

        return newWallet;
    }

    public Wallet updateWallet(Long id, Wallet walletUpdate) {

        Wallet wallet = getWalletById(id);

        Customer customer = customerService.getCustomerById(walletUpdate.getCustomerId());

        wallet.setUpdatedAt(LocalDateTime.now());
        wallet.setCurrentBalance(walletUpdate.getCurrentBalance());
        wallet.setPreviousBalance(walletUpdate.getPreviousBalance());
        wallet.setCustomerId(walletUpdate.getCustomerId());
        wallet.setCustomer(customer);
        wallet.setStatus(walletUpdate.getStatus());
        wallet.setPreviousBalance(wallet.getPreviousBalance());

        return walletRepository.save(wallet);

    }

    public Page<Wallet> getAllWallets(Pageable pageable) {

        Page<Wallet> list = walletRepository.findAll(pageable);

        if (list.getContent().isEmpty()) {
            throw new ResourceNotFoundException("No wallets found");
        }

        return list;
    }

    public List<Wallet> getWalletByCustomerId(Long id) {
        return walletRepository.findAllWalletsByCustomerId(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format("Wallet not found with id: %d", id)));
    }

}
