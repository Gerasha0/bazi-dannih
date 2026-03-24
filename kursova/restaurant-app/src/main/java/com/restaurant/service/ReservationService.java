package com.restaurant.service;

import com.restaurant.entity.Reservation;
import com.restaurant.entity.RestaurantTable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationService {
    List<Reservation> findAll();
    List<Reservation> findUpcoming(Integer restaurantId);
    Optional<Reservation> findById(Integer id);

    /** Повертає вільні столики в ресторані для вказаного інтервалу */
    List<RestaurantTable> findAvailableTables(Integer restaurantId,
                                              LocalDateTime from,
                                              LocalDateTime to);

    Reservation createReservation(Integer restaurantId, Integer tableId,
                                  Integer customerId, LocalDateTime from,
                                  LocalDateTime to, int partySize, String notes);

    Reservation cancel(Integer id);
    Reservation complete(Integer id);
    void delete(Integer id);
}
