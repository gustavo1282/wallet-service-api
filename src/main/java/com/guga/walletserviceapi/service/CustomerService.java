package com.guga.walletserviceapi.service;


import com.guga.walletserviceapi.config.ResourceNotFoundException;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class CustomerService {

    @Autowired
    private final CustomerRepository customerRepository;

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + String.valueOf(id)));
    }

    /***
     * Check if customer exists by excepting null
     * @param id
     * @return Customer or null
     */
    public Customer existsCustomer(Long id) {

        return customerRepository.findById(id).orElse(null);

    }

    @Transactional
    public Customer saveCustomer(Customer customer) {
        Customer newCustomer = customerRepository.save(customer);

        if(newCustomer.getCustomerId() == null) {
            throw new ResourceNotFoundException("Error saving customer");
        }

        return newCustomer;
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customerUpdate) {

        Customer customer = getCustomerById(id);

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

    public Page<Customer> getAllCustomers(Pageable pageable) {

        Page<Customer> list = customerRepository.findAll(pageable);

        if (list.getContent().isEmpty()) {
            throw new ResourceNotFoundException("No wallets found");
        }

        return list;

    }

}
