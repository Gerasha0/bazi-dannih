package com.restaurant.service;

import com.restaurant.entity.MenuItem;
import com.restaurant.entity.Order;
import com.restaurant.entity.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    List<Order> findAll();
    List<Order> findByRestaurant(Integer restaurantId);
    List<Order> findActive(Integer restaurantId);
    Optional<Order> findById(Integer id);

    /** Створює нове замовлення */
    Order createOrder(Integer restaurantId, Integer tableId,
                      Integer customerId, Integer employeeId);

    /** Додає страву до замовлення */
    Order addItem(Integer orderId, Integer menuItemId, int quantity);

    /** Видаляє позицію з замовлення */
    Order removeItem(Integer orderId, Integer orderItemId);

    /** Змінює статус замовлення */
    Order changeStatus(Integer orderId, OrderStatus newStatus);

    /** Оновлює прив'язку столика / офіціанта / клієнта */
    Order updateAssignment(Integer orderId, Integer tableId, Integer employeeId, Integer customerId);

    /** Завершує та оплачує замовлення, нараховує бали */
    Order payOrder(Integer orderId);

    /** Звільняє столик: знімає флаг is_occupied (люди встали) */
    void freeTable(Integer tableId);

    void delete(Integer id);
}
