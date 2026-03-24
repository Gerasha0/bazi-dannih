package com.restaurant.service.impl;

import com.restaurant.entity.Customer;
import com.restaurant.exception.EntityNotFoundException;
import com.restaurant.repository.CustomerRepository;
import com.restaurant.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAllByOrderByLastNameAscFirstNameAsc();
    }

    @Override
    public Optional<Customer> findById(Integer id) {
        return customerRepository.findById(id);
    }

    @Override
    public Optional<Customer> findByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }

    @Override
    @Transactional
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public Customer update(Integer id, Customer data) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клієнт", id));
        existing.setFirstName(data.getFirstName());
        existing.setLastName(data.getLastName());
        existing.setPhone(data.getPhone());
        existing.setEmail(data.getEmail());
        return customerRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        customerRepository.deleteById(id);
    }

    @Override
    public List<Customer> search(String query) {
        return customerRepository
                .findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrPhoneContaining(
                        query, query, query);
    }

    @Override
    @Transactional
    public void addLoyaltyPoints(Integer customerId, int points) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Клієнт", customerId));
        c.setLoyaltyPoints(c.getLoyaltyPoints() + points);
        customerRepository.save(c);
    }
}
