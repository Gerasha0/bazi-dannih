package com.restaurant.service.impl;

import com.restaurant.entity.*;
import com.restaurant.exception.EntityNotFoundException;
import com.restaurant.exception.RestaurantAppException;
import com.restaurant.repository.*;
import com.restaurant.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final Set<OrderStatus> ACTIVE_STATUSES =
            EnumSet.of(OrderStatus.PENDING, OrderStatus.PREPARING,
                       OrderStatus.READY,   OrderStatus.SERVED);

    private final OrderRepository           orderRepository;
    private final RestaurantRepository      restaurantRepository;
    private final RestaurantTableRepository tableRepository;
    private final CustomerRepository    customerRepository;
    private final EmployeeRepository    employeeRepository;
    private final MenuItemRepository    menuItemRepository;

    @Override
    public List<Order> findAll() {
        return orderRepository.findAllWithItems();
    }

    @Override
    public List<Order> findByRestaurant(Integer restaurantId) {
        return orderRepository.findByRestaurantIdWithItems(restaurantId);
    }

    @Override
    public List<Order> findActive(Integer restaurantId) {
        return orderRepository.findByRestaurantIdAndStatusIn(
                restaurantId, List.copyOf(ACTIVE_STATUSES));
    }

    @Override
    public Optional<Order> findById(Integer id) {
        return orderRepository.findById(id);
    }

    @Override
    @Transactional
    public Order createOrder(Integer restaurantId, Integer tableId,
                             Integer customerId, Integer employeeId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Ресторан", restaurantId));

        RestaurantTable table = tableId != null
                ? tableRepository.findById(tableId)
                        .orElseThrow(() -> new EntityNotFoundException("Столик", tableId))
                : null;

        // Перевірка: столик не може бути зайнятий активним замовленням
        if (table != null) {
            boolean occupied = !orderRepository
                    .findByTableIdAndStatusIn(tableId, List.copyOf(ACTIVE_STATUSES))
                    .isEmpty();
            if (occupied || table.isOccupied()) {
                throw new RestaurantAppException(
                        "Столик №" + table.getTableNumber()
                        + " вже зайнятий. Спочатку звільніть столик або завершіть/скасуйте поточне замовлення.");
            }
        }

        Customer customer = customerId != null
                ? customerRepository.findById(customerId).orElse(null)
                : null;

        Employee employee = employeeId != null
                ? employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new EntityNotFoundException("Співробітник", employeeId))
                : null;

        Order order = Order.builder()
                .restaurant(restaurant)
                .table(table)
                .customer(customer)
                .employee(employee)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();
        Order saved = orderRepository.save(order);
        // Маркуємо столик як фізично зайнятий
        if (table != null) {
            table.setOccupied(true);
            tableRepository.save(table);
        }
        return saved;
    }

    @Override
    @Transactional
    public Order addItem(Integer orderId, Integer menuItemId, int quantity) {
        Order order = getActiveOrder(orderId);
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new EntityNotFoundException("Страва", menuItemId));

        // Якщо така страва вже є — збільшуємо кількість
        order.getItems().stream()
                .filter(i -> i.getMenuItem().getId().equals(menuItemId))
                .findFirst()
                .ifPresentOrElse(
                        i -> i.setQuantity(i.getQuantity() + quantity),
                        () -> order.getItems().add(OrderItem.builder()
                                .order(order)
                                .menuItem(menuItem)
                                .quantity(quantity)
                                .unitPrice(menuItem.getPrice())
                                .build())
                );

        order.recalcTotal();
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order removeItem(Integer orderId, Integer orderItemId) {
        Order order = getActiveOrder(orderId);
        order.getItems().removeIf(i -> i.getId().equals(orderItemId));
        order.recalcTotal();
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order updateAssignment(Integer orderId, Integer tableId, Integer employeeId, Integer customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Замовлення", orderId));

        RestaurantTable table = tableId != null
                ? tableRepository.findById(tableId)
                        .orElseThrow(() -> new EntityNotFoundException("Столик", tableId))
                : null;

        // Перевірка: столик повинен належати тому ж закладу
        if (table != null && !table.getRestaurant().getId().equals(order.getRestaurant().getId())) {
            throw new RestaurantAppException(
                    "Столик №" + table.getTableNumber()
                    + " належить іншому закладу і не може бути призначений цьому замовленню.");
        }

        Employee employee = employeeId != null
                ? employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new EntityNotFoundException("Співробітник", employeeId))
                : null;
        Customer customer = customerId != null
                ? customerRepository.findById(customerId).orElse(null)
                : null;

        order.setTable(table);
        order.setEmployee(employee);
        order.setCustomer(customer);
        Order saved = orderRepository.save(order);
        // Маркуємо новий столик як зайнятий
        if (table != null) {
            table.setOccupied(true);
            tableRepository.save(table);
        }
        return saved;
    }

    @Override
    @Transactional
    public Order changeStatus(Integer orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Замовлення", orderId));
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order payOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Замовлення", orderId));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new RestaurantAppException("Замовлення #" + orderId + " вже оплачено");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RestaurantAppException("Неможливо оплатити скасоване замовлення");
        }

        order.setStatus(OrderStatus.PAID);
        order.setCompletedAt(LocalDateTime.now());

        // Нарахування 1 бал за кожні 10 грн
        if (order.getCustomer() != null) {
            int points = order.getTotalAmount().divide(BigDecimal.TEN).intValue();
            Customer c = order.getCustomer();
            c.setLoyaltyPoints(c.getLoyaltyPoints() + points);
        }
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void freeTable(Integer tableId) {
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new EntityNotFoundException("Столик", tableId));
        table.setOccupied(false);
        tableRepository.save(table);
        // Відв'язуємо столик від усіх замовлень, що на нього посилаються
        orderRepository.findByTableId(tableId).forEach(o -> {
            o.setTable(null);
            orderRepository.save(o);
        });
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        orderRepository.deleteById(id);
    }

    private Order getActiveOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Замовлення", orderId));
        if (!ACTIVE_STATUSES.contains(order.getStatus())) {
            throw new RestaurantAppException(
                    "Замовлення #" + orderId + " не активне (статус: " + order.getStatus() + ")");
        }
        return order;
    }
}
