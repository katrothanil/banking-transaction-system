package com.anil.banking.service;

import com.anil.banking.dto.CreateCustomerRequest;
import com.anil.banking.dto.CustomerResponse;
import com.anil.banking.entity.Customer;
import com.anil.banking.exception.DuplicateResourceException;
import com.anil.banking.exception.ResourceNotFoundException;
import com.anil.banking.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        String email = request.email().trim().toLowerCase();
        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("A customer with email " + email + " already exists");
        }
        Customer customer = new Customer(request.fullName().trim(), email, request.phone());
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        return CustomerResponse.from(findCustomer(id));
    }

    Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + id + " not found"));
    }
}
