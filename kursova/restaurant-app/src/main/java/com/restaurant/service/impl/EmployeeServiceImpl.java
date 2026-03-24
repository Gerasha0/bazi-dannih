package com.restaurant.service.impl;

import com.restaurant.entity.Employee;
import com.restaurant.exception.EntityNotFoundException;
import com.restaurant.repository.EmployeeRepository;
import com.restaurant.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Override
    public List<Employee> findAll() {
        return employeeRepository.findAllByOrderByRestaurantNameAscLastNameAsc();
    }

    @Override
    public List<Employee> findByRestaurant(Integer restaurantId) {
        return employeeRepository.findByRestaurantId(restaurantId);
    }

    @Override
    public List<Employee> findActiveByRestaurant(Integer restaurantId) {
        return employeeRepository.findByRestaurantIdAndActiveTrue(restaurantId);
    }

    @Override
    public Optional<Employee> findById(Integer id) {
        return employeeRepository.findById(id);
    }

    @Override
    @Transactional
    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public Employee update(Integer id, Employee data) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Співробітник", id));
        existing.setFirstName(data.getFirstName());
        existing.setLastName(data.getLastName());
        existing.setPhone(data.getPhone());
        existing.setEmail(data.getEmail());
        existing.setPosition(data.getPosition());
        existing.setRestaurant(data.getRestaurant());
        existing.setSalary(data.getSalary());
        existing.setHireDate(data.getHireDate());
        existing.setActive(data.isActive());
        return employeeRepository.save(existing);
    }

    @Override
    @Transactional
    public void deactivate(Integer id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Співробітник", id));
        emp.setActive(false);
        employeeRepository.save(emp);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        employeeRepository.deleteById(id);
    }

    @Override
    public List<Employee> search(String query) {
        return employeeRepository
                .findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(query, query);
    }
}
