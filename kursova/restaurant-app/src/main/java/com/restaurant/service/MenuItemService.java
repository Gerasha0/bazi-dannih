package com.restaurant.service;

import com.restaurant.dto.MenuItemDto;
import com.restaurant.dto.PopularDishDto;
import com.restaurant.entity.MenuItem;
import java.util.List;
import java.util.Optional;

public interface MenuItemService {
    List<MenuItem> findAll();
    List<MenuItem> findAllIncludingUnavailable();
    List<MenuItem> findAvailable();
    List<MenuItem> findByCategory(Integer categoryId);
    Optional<MenuItem> findById(Integer id);
    MenuItem save(MenuItemDto dto);
    MenuItem update(Integer id, MenuItemDto dto);
    void delete(Integer id);
    List<MenuItem> search(String name);
    List<PopularDishDto> getTopPopularDishes(int limit);
}
