package com.guga.walletserviceapi.service;

import java.time.LocalDateTime;
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
import com.guga.walletserviceapi.service.common.DataImportService;
import com.guga.walletserviceapi.service.common.ImportSummary;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CustomerService implements IWalletApiService {

    private final CustomerRepository customerRepository;
    private final ParamAppService paramAppService;
    private final DataImportService importService;


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

        Customer customer = getCustomerById(id);

        customer.setCpf(customerUpdate.getCpf());
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setPhoneNumber(customerUpdate.getPhoneNumber());
        customer.setEmail(customerUpdate.getEmail());
        customer.setFirstName(customerUpdate.getFirstName());
        customer.setLastName(customerUpdate.getLastName());
        customer.setBirthDate(customerUpdate.getBirthDate());
        customer.setDocumentId(customerUpdate.getDocumentId());
        customer.setStatus(customerUpdate.getStatus());

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

    public ImportSummary importCustomers(MultipartFile file) {        
        return importService.importJson(file, new TypeReference<List<Customer>>() {}, customerRepository);
    }

    @Override
    public Long nextIdGenerate() {
        return paramAppService
            .getNextSequenceId(ParamApp.SEQ_CUSTOMER_ID)
            .getValueLong();
    }
    
}
