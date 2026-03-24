package com.restaurant.service.impl;

import com.restaurant.entity.*;
import com.restaurant.exception.EntityNotFoundException;
import com.restaurant.exception.RestaurantAppException;
import com.restaurant.repository.*;
import com.restaurant.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository     reservationRepository;
    private final RestaurantRepository      restaurantRepository;
    private final RestaurantTableRepository tableRepository;
    private final CustomerRepository        customerRepository;

    @Override
    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    @Override
    public List<Reservation> findUpcoming(Integer restaurantId) {
        return reservationRepository.findUpcoming(restaurantId, LocalDateTime.now());
    }

    @Override
    public Optional<Reservation> findById(Integer id) {
        return reservationRepository.findById(id);
    }

    @Override
    public List<RestaurantTable> findAvailableTables(Integer restaurantId,
                                                     LocalDateTime from,
                                                     LocalDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new RestaurantAppException("Невірний проміжок часу для пошуку столиків");
        }
        return tableRepository.findAvailableTables(restaurantId, from, to);
    }

    @Override
    @Transactional
    public Reservation createReservation(Integer restaurantId, Integer tableId,
                                         Integer customerId, LocalDateTime from,
                                         LocalDateTime to, int partySize, String notes) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Ресторан", restaurantId));

        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new EntityNotFoundException("Столик", tableId));

        if (table.getCapacity() < partySize) {
            throw new RestaurantAppException(
                    "Столик розрахований на " + table.getCapacity()
                    + " місць, запитано " + partySize);
        }

        // Перевірка доступності
        List<RestaurantTable> available = tableRepository.findAvailableTables(restaurantId, from, to);
        boolean isAvailable = available.stream().anyMatch(t -> t.getId().equals(tableId));
        if (!isAvailable) {
            throw new RestaurantAppException("Столик вже заброньований на вказаний час");
        }

        Customer customer = customerId != null
                ? customerRepository.findById(customerId).orElse(null)
                : null;

        Reservation reservation = Reservation.builder()
                .restaurant(restaurant)
                .table(table)
                .customer(customer)
                .reservationTime(from)
                .endTime(to)
                .partySize(partySize)
                .status(ReservationStatus.CONFIRMED)
                .notes(notes)
                .build();

        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public Reservation cancel(Integer id) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Бронювання", id));
        r.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(r);
    }

    @Override
    @Transactional
    public Reservation complete(Integer id) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Бронювання", id));
        r.setStatus(ReservationStatus.COMPLETED);
        return reservationRepository.save(r);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        reservationRepository.deleteById(id);
    }
}
