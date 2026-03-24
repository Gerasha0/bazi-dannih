package com.restaurant.service.impl;

import com.restaurant.dto.MenuItemDto;
import com.restaurant.dto.PopularDishDto;
import com.restaurant.entity.Category;
import com.restaurant.entity.MenuItem;
import com.restaurant.exception.EntityNotFoundException;
import com.restaurant.repository.CategoryRepository;
import com.restaurant.repository.MenuItemRepository;
import com.restaurant.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public List<MenuItem> findAll() {
        return menuItemRepository.findAllAvailableOrdered();
    }

    @Override
    public List<MenuItem> findAllIncludingUnavailable() {
        return menuItemRepository.findAllOrderedByCategoryAndName();
    }

    @Override
    public List<MenuItem> findAvailable() {
        return menuItemRepository.findByAvailableTrue();
    }

    @Override
    public List<MenuItem> findByCategory(Integer categoryId) {
        return menuItemRepository.findByCategoryId(categoryId);
    }

    @Override
    public Optional<MenuItem> findById(Integer id) {
        return menuItemRepository.findById(id);
    }

    @Override
    @Transactional
    public MenuItem save(MenuItemDto dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Категорія", dto.getCategoryId()));

        MenuItem item = MenuItem.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .category(category)
                .available(dto.isAvailable())
                .build();
        return menuItemRepository.save(item);
    }

    @Override
    @Transactional
    public MenuItem update(Integer id, MenuItemDto dto) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Страва", id));

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Категорія", dto.getCategoryId()));
            item.setCategory(category);
        }
        if (dto.getName() != null)        item.setName(dto.getName());
        if (dto.getDescription() != null) item.setDescription(dto.getDescription());
        if (dto.getPrice() != null)       item.setPrice(dto.getPrice());
        item.setAvailable(dto.isAvailable());

        return menuItemRepository.save(item);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        menuItemRepository.deleteById(id);
    }

    @Override
    public List<MenuItem> search(String name) {
        return menuItemRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public List<PopularDishDto> getTopPopularDishes(int limit) {
        List<Object[]> raw = menuItemRepository.findPopularDishesRaw(PageRequest.of(0, limit));
        return raw.stream().map(row -> new PopularDishDto(
                (Integer)    row[0],
                (String)     row[1],
                (String)     row[2],
                (BigDecimal) row[3],
                ((Number)    row[4]).longValue(),
                (BigDecimal) row[5]
        )).toList();
    }
}
