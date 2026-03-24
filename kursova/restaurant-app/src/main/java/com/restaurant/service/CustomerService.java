package com.restaurant.service;

import com.restaurant.entity.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerService {
    List<Customer> findAll();
    Optional<Customer> findById(Integer id);
    Optional<Customer> findByPhone(String phone);
    Customer save(Customer customer);
    Customer update(Integer id, Customer data);
    void delete(Integer id);
    List<Customer> search(String query);
    void addLoyaltyPoints(Integer customerId, int points);
}
