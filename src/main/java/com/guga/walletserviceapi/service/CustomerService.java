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
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

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
        Customer newCustomer = customerRepository.save(customer);

        if (newCustomer.getCustomerId() == null) {
            throw new ResourceNotFoundException("Error saving customer");
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadCsvAndSave(MultipartFile file) throws Exception {

        try {
            ObjectMapper mapper = FileUtils.instanceObjectMapper();

            TypeReference<List<Customer>> customerTypeRef = new TypeReference<List<Customer>>() { };

            List<Customer> customers = mapper.readValue(file.getInputStream(), customerTypeRef);

            for (int i = 0; i < customers.size(); i += GlobalHelper.BATCH_SIZE) {

                int end = Math.min(customers.size(), i + GlobalHelper.BATCH_SIZE);
                
                List<Customer> lote = customers.subList(i, end);

                customerRepository.saveAll(lote);

            }
        } catch (Exception e) {
            throw new ResourceBadRequestException(e.getMessage());
        }
    }
    
}
