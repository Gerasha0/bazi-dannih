package com.restaurant.repository;

import com.restaurant.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Integer> {

    List<RestaurantTable> findByRestaurantIdOrderByTableNumber(Integer restaurantId);

    List<RestaurantTable> findByRestaurantIdAndCapacityGreaterThanEqualOrderByCapacity(
            Integer restaurantId, int minCapacity);

    /**
     * Вільні столики в ресторані на заданий час:
     * повертає столики, у яких немає активного бронювання для переданого інтервалу.
     */
    @Query("""
        SELECT t FROM RestaurantTable t
        WHERE t.restaurant.id = :restaurantId
          AND t.id NOT IN (
            SELECT r.table.id FROM Reservation r
            WHERE r.restaurant.id = :restaurantId
              AND r.status = com.restaurant.entity.ReservationStatus.CONFIRMED
              AND r.reservationTime < :endTime
              AND r.endTime        > :startTime
          )
        ORDER BY t.tableNumber
        """)
    List<RestaurantTable> findAvailableTables(
            @Param("restaurantId") Integer restaurantId,
            @Param("startTime")   LocalDateTime startTime,
            @Param("endTime")     LocalDateTime endTime);
}
