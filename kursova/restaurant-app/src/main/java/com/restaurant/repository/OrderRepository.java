package com.restaurant.repository;

import com.restaurant.entity.Order;
import com.restaurant.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByRestaurantIdOrderByCreatedAtDesc(Integer restaurantId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCustomerId(Integer customerId);

    List<Order> findByRestaurantIdAndStatusIn(Integer restaurantId, List<OrderStatus> statuses);

    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);

    List<Order> findByRestaurantIdAndCreatedAtBetweenAndStatus(
            Integer restaurantId, LocalDateTime from, LocalDateTime to, OrderStatus status);

    /**
     * Звіт про продажі за ресторанами та заданим проміжком дат.
     * Повертає [restaurantName, date, ordersCount, itemsSold, totalRevenue]
     */
    @Query("""
        SELECT r.name,
               CAST(o.createdAt AS LocalDate),
               COUNT(DISTINCT o.id),
               SUM(oi.quantity),
               SUM(oi.quantity * oi.unitPrice)
        FROM Order o
        JOIN o.restaurant r
        JOIN o.items oi
        WHERE o.status = com.restaurant.entity.OrderStatus.PAID
          AND o.createdAt >= :from
          AND o.createdAt <  :to
        GROUP BY r.id, r.name, CAST(o.createdAt AS LocalDate)
        ORDER BY r.name, CAST(o.createdAt AS LocalDate)
        """)
    List<Object[]> findSalesReport(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items ORDER BY o.createdAt DESC")
    List<Order> findAllWithItems();

    @Query("""
        SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items
        WHERE o.restaurant.id = :restaurantId
        ORDER BY o.createdAt DESC
        """)
    List<Order> findByRestaurantIdWithItems(@Param("restaurantId") Integer restaurantId);

    // ---- Зайнятість столиків ------------------------------------------------

    /** IDs столиків з активними замовленнями прямо зараз */
    @Query("""
        SELECT DISTINCT o.table.id FROM Order o
        WHERE o.restaurant.id = :restaurantId
          AND o.status IN :statuses
          AND o.table IS NOT NULL
        """)
    List<Integer> findOccupiedTableIds(
            @Param("restaurantId") Integer restaurantId,
            @Param("statuses")     Collection<OrderStatus> statuses);

    /** IDs столиків, що мали незасохлі замовлення впродовж інтервалу [from, to].
     *  Умова перетину: замовлення почалось до кінця інтервалу І
     *  завершилось після початку інтервалу (або ще не завершилось).
     *  Додатковий фільтр table.restaurant.id = restaurantId захищає від
     *  ситуацій коли стіл був помилково переназначений між закладами. */
    @Query("""
        SELECT DISTINCT o.table.id FROM Order o
        WHERE o.restaurant.id         = :restaurantId
          AND o.table.restaurant.id   = :restaurantId
          AND o.status               <> com.restaurant.entity.OrderStatus.CANCELLED
          AND o.table IS NOT NULL
          AND o.createdAt            <= :to
          AND (o.completedAt IS NULL   OR o.completedAt >= :from)
        """)
    List<Integer> findOccupiedTableIdsInPeriod(
            @Param("restaurantId") Integer restaurantId,
            @Param("from")         LocalDateTime from,
            @Param("to")           LocalDateTime to);

    /** Перевірка: чи є активне замовлення за конкретним столиком */
    List<Order> findByTableIdAndStatusIn(Integer tableId, Collection<OrderStatus> statuses);

    /** Всі замовлення, що посилаються на даний столик (незалежно від статусу) */
    List<Order> findByTableId(Integer tableId);
}
