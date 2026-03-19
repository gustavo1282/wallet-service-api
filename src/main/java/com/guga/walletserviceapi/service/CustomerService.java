package com.guga.walletserviceapi.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.guga.walletserviceapi.exception.ResourceNotFoundException;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.repository.CustomerRepository;
import com.guga.walletserviceapi.service.common.DataPersistenceService;
import com.guga.walletserviceapi.service.common.PersistenceSummary;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CustomerService implements IWalletApiService {

    private final CustomerRepository customerRepository;
    private final ParamAppService paramAppService;
    private final DataPersistenceService importService;


    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + String.valueOf(id)));
    }

    /***
     * Check if customer exists by excepting null
     * 
     * @param id
     * @return Customer or null
     */
    public Customer existsCustomer(Long id) {

        if (id == null || id < 1)
            return null;

        return customerRepository.findById(id).orElse(null);

    }

    @Transactional
    public Customer saveCustomer(Customer customer) {
        customer.setCustomerId(nextIdGenerate());

        Customer newCustomer = customerRepository.save(customer);

        if (newCustomer.getCustomerId() == null) {
            throw new ResourceNotFoundException("Error saving customer: " + customer.toString());
        }

        return newCustomer;
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customerUpdate) {
        if (customerUpdate == null) {
            throw new IllegalArgumentException("Customer update data cannot be null");
        }

        Customer customer = getCustomerById(id);
        
        if (customerUpdate.getStatus() != null) {
            customer.setStatus(customerUpdate.getStatus());
        }
        if (customerUpdate.getPhoneNumber() != null) {
            customer.setPhoneNumber(customerUpdate.getPhoneNumber());
        }
        if (customerUpdate.getEmail() != null) {
            customer.setEmail(customerUpdate.getEmail());
        }
        if (customerUpdate.getBirthDate() != null) {
            customer.setBirthDate(customerUpdate.getBirthDate());
        }

        return customerRepository.save(customer);
    }

    public Page<Customer> filterByStatus(Status status, Pageable pageable) {

        Page<Customer> findResult;
        if (status == null) {
            findResult = customerRepository.findAll(pageable);
        }
        else {
            findResult = customerRepository.findByStatus(status, pageable);
        }

        if (findResult.isEmpty() || !findResult.hasContent()) {
            throw new ResourceNotFoundException("Customers not found");
        }

        return findResult;

    }

    public PersistenceSummary importCustomers(MultipartFile file) {        
        return importService.importJsonFromUpload(file, new TypeReference<List<Customer>>() {}, customerRepository);
    }

    @Override
    public Long nextIdGenerate() {
        return paramAppService
            .getNextSequenceId(ParamApp.SEQ_CUSTOMER_ID)
            .getValueLong();
    }
    
}
