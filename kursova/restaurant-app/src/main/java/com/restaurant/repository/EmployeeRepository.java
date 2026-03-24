package com.restaurant.repository;

import com.restaurant.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    List<Employee> findByRestaurantId(Integer restaurantId);

    List<Employee> findByRestaurantIdAndActiveTrue(Integer restaurantId);

    List<Employee> findByPositionId(Integer positionId);

    List<Employee> findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
            String lastName, String firstName);

    List<Employee> findAllByOrderByRestaurantNameAscLastNameAsc();
}
