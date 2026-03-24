package com.restaurant.repository;

import com.restaurant.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

    List<MenuItem> findByCategoryId(Integer categoryId);

    List<MenuItem> findByAvailableTrue();

    List<MenuItem> findByNameContainingIgnoreCase(String name);

    @Query("SELECT m FROM MenuItem m WHERE m.available = true ORDER BY m.category.name, m.name")
    List<MenuItem> findAllAvailableOrdered();

    @Query("SELECT m FROM MenuItem m ORDER BY m.category.name, m.name")
    List<MenuItem> findAllOrderedByCategoryAndName();

    /**
     * Топ-N найпопулярніших страв (за кількістю замовлень зі статусом 'paid').
     * Повертає масиви об'єктів, які потім маппимо в PopularDishDto в сервісі.
     */
    @Query("""
        SELECT m.id, m.name, c.name, m.price,
               SUM(oi.quantity) AS timesOrdered,
               SUM(oi.quantity * oi.unitPrice) AS revenue
        FROM OrderItem oi
        JOIN oi.menuItem m
        JOIN m.category c
        JOIN oi.order o
        WHERE o.status = com.restaurant.entity.OrderStatus.PAID
        GROUP BY m.id, m.name, c.name, m.price
        ORDER BY timesOrdered DESC
        """)
    List<Object[]> findPopularDishesRaw(org.springframework.data.domain.Pageable pageable);
}
