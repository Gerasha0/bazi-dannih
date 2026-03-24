package com.restaurant.service.impl;

import com.restaurant.entity.Restaurant;
import com.restaurant.exception.EntityNotFoundException;
import com.restaurant.repository.RestaurantRepository;
import com.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Override
    public List<Restaurant> findAll() {
        return restaurantRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Optional<Restaurant> findById(Integer id) {
        return restaurantRepository.findById(id);
    }

    @Override
    @Transactional
    public Restaurant save(Restaurant restaurant) {
        return restaurantRepository.save(restaurant);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Restaurant r = restaurantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ресторан", id));
        restaurantRepository.delete(r);
    }

    @Override
    public List<Restaurant> search(String query) {
        return restaurantRepository.findByNameContainingIgnoreCase(query);
    }
}
