package com.restaurant.repository;

import com.restaurant.entity.Reservation;
import com.restaurant.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    List<Reservation> findByRestaurantIdAndStatus(Integer restaurantId, ReservationStatus status);

    List<Reservation> findByCustomerId(Integer customerId);

    List<Reservation> findByReservationTimeBetweenOrderByReservationTimeAsc(
            LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT rv FROM Reservation rv
        WHERE rv.restaurant.id = :restaurantId
          AND rv.status = com.restaurant.entity.ReservationStatus.CONFIRMED
          AND rv.reservationTime >= :now
        ORDER BY rv.reservationTime
        """)
    List<Reservation> findUpcoming(
            @Param("restaurantId") Integer restaurantId,
            @Param("now")          LocalDateTime now);
}
