package com.guga.walletserviceapi.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger LOGGER = LogManager.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final ParamAppService paramAppService;
    private final DataPersistenceService importService;


    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        LOGGER.info("CUSTOMER_SERVICE_GET_BY_ID_ENTRY | customerId={}", id);

        Customer customer = customerRepository.findById(id).orElse(null);

        if (customer == null) {
            LOGGER.warn("CUSTOMER_SERVICE_GET_BY_ID_DECISION | customerId={} decision=NOT_FOUND", id);
            throw new ResourceNotFoundException("Customer not found with id: " + String.valueOf(id));
        }

        LOGGER.info("CUSTOMER_SERVICE_GET_BY_ID_SUCCESS | customerId={}", customer.getCustomerId());
        return customer;
    }

    /***
     * Check if customer exists by excepting null
     * 
     * @param id
     * @return Customer or null
     */
    @Transactional(readOnly = true)
    public Customer existsCustomer(Long id) {

        if (id == null || id < 1)
            return null;

        return customerRepository.findById(id).orElse(null);

    }

    @Transactional(rollbackFor = Exception.class)
    public Customer saveCustomer(Customer customer) {
        LOGGER.info("CUSTOMER_SERVICE_SAVE_ENTRY");

        Long nextId = nextIdGenerate();
        LOGGER.info("CUSTOMER_SERVICE_SAVE_AUX_RESULT | generatedCustomerId={}", nextId);
        customer.setCustomerId(nextId);
        Customer newCustomer = customerRepository.save(customer);

        if (newCustomer != null && newCustomer.getCustomerId() == null) {
            LOGGER.warn("CUSTOMER_SERVICE_SAVE_DECISION | persistedIdIsNull=true");
            throw new ResourceNotFoundException("Error saving customer: " + customer.toString());
        }

        LOGGER.info("CUSTOMER_SERVICE_SAVE_SUCCESS | customerId={}", newCustomer.getCustomerId());
        return newCustomer;
    }

    @Transactional(rollbackFor = Exception.class)
    public Customer updateCustomer(Long id, Customer customerUpdate) {
        LOGGER.info("CUSTOMER_SERVICE_UPDATE_ENTRY | customerId={}", id);

        Customer customer = getCustomerById(id);

        boolean statusChanged = false;
        boolean phoneChanged = false;
        boolean emailChanged = false;
        boolean birthDateChanged = false;
        
        if (customerUpdate.getStatus() != null) {
            customer.setStatus(customerUpdate.getStatus());
            statusChanged = true;
        }
        if (customerUpdate.getPhoneNumber() != null) {
            customer.setPhoneNumber(customerUpdate.getPhoneNumber());
            phoneChanged = true;
        }
        if (customerUpdate.getEmail() != null) {
            customer.setEmail(customerUpdate.getEmail());
            emailChanged = true;
        }
        if (customerUpdate.getBirthDate() != null) {
            customer.setBirthDate(customerUpdate.getBirthDate());
            birthDateChanged = true;
        }

        LOGGER.info(
            "CUSTOMER_SERVICE_UPDATE_DECISION | customerId={} fieldsChanged=status:{},phone:{},email:{},birthDate:{}",
            customer.getCustomerId(), statusChanged, phoneChanged, emailChanged, birthDateChanged
        );

        Customer updated = customerRepository.save(customer);
        LOGGER.info("CUSTOMER_SERVICE_UPDATE_SUCCESS | customerId={}", updated.getCustomerId());
        return updated;
    }

    @Transactional(readOnly = true)
    public Page<Customer> filterByStatus(Status status, Pageable pageable) {
        LOGGER.info(
            "CUSTOMER_SERVICE_FILTER_ENTRY | status={} page={} size={} sort={}",
            status, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()
        );

        Page<Customer> findResult;
        if (status == null) {
            LOGGER.info("CUSTOMER_SERVICE_FILTER_DECISION | branch=findAll");
            findResult = customerRepository.findAll(pageable);
        }
        else {
            LOGGER.info("CUSTOMER_SERVICE_FILTER_DECISION | branch=findByStatus");
            findResult = customerRepository.findByStatus(status, pageable);
        }

        if (findResult.isEmpty() || !findResult.hasContent()) {
            LOGGER.warn("CUSTOMER_SERVICE_FILTER_DECISION | status={} decision=EMPTY_RESULT", status);
            throw new ResourceNotFoundException("Customers not found");
        }

        LOGGER.info("CUSTOMER_SERVICE_FILTER_SUCCESS | status={} rows={}", status, findResult.getNumberOfElements());
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
