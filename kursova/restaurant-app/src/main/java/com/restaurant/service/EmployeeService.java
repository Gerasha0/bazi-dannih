package com.restaurant.service;

import com.restaurant.entity.Employee;
import java.util.List;
import java.util.Optional;

public interface EmployeeService {
    List<Employee> findAll();
    List<Employee> findByRestaurant(Integer restaurantId);
    List<Employee> findActiveByRestaurant(Integer restaurantId);
    Optional<Employee> findById(Integer id);
    Employee save(Employee employee);
    Employee update(Integer id, Employee data);
    void deactivate(Integer id);
    void delete(Integer id);
    List<Employee> search(String query);
}
