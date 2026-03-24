package com.restaurant.dto;

import com.restaurant.entity.RestaurantTable;

/**
 * Статус столика: вільний або зайнятий активним замовленням.
 * Використовується у вкладці "Зайнятість столиків" звітів.
 */
public record TableStatusDto(
        RestaurantTable table,
        boolean         occupied,
        String          details    // напр. "Замовл. #7 — Подано" або "" якщо вільно
) {}
