package com.restaurant.service;

import com.restaurant.dto.PopularDishDto;
import com.restaurant.dto.SalesReportRowDto;
import com.restaurant.dto.TableStatusDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReportService {
    /** Звіт про продажі за заданий період */
    List<SalesReportRowDto> getSalesReport(LocalDate from, LocalDate to);

    /** Топ-N популярних страв */
    List<PopularDishDto> getTopDishes(int limit);

    /**
     * Поточна зайнятість столиків закладу.
     * Столик вважається зайнятим, якщо за ним є замовлення зі статусом
     * PENDING / PREPARING / READY / SERVED.
     */
    List<TableStatusDto> getTableOccupancy(Integer restaurantId);

    /**
     * Зайнятість столиків закладу за заданий часовий інтервал.
     * Столик вважається зайнятим, якщо є хоча б одне незасохле замовлення
     * (не CANCELLED), яке перетиналося з інтервалом [from, to].
     */
    List<TableStatusDto> getTableOccupancyInPeriod(Integer restaurantId,
                                                   LocalDateTime from,
                                                   LocalDateTime to);
}
