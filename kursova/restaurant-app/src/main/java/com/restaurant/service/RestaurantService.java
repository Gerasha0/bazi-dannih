package com.restaurant.service;

import com.restaurant.entity.Restaurant;
import java.util.List;
import java.util.Optional;

public interface RestaurantService {
    List<Restaurant> findAll();
    Optional<Restaurant> findById(Integer id);
    Restaurant save(Restaurant restaurant);
    void delete(Integer id);
    List<Restaurant> search(String query);
}
