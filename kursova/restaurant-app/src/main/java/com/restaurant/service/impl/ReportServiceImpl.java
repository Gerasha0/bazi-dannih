package com.restaurant.service.impl;

import com.restaurant.dto.PopularDishDto;
import com.restaurant.dto.SalesReportRowDto;
import com.restaurant.dto.TableStatusDto;
import com.restaurant.entity.OrderStatus;
import com.restaurant.entity.RestaurantTable;
import com.restaurant.repository.MenuItemRepository;
import com.restaurant.repository.OrderRepository;
import com.restaurant.repository.RestaurantTableRepository;
import com.restaurant.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    /**
     * Статуси, при яких стіл вважається зайнятим.
     * PAID не входить — оплачене замовлення вважається завершеним:
     * якщо клієнт ще фізично сидить, але вже оплатив, стіл можна
     * взяти під наступне замовлення тільки після явного закриття.
     * Поки що вважаємо: PAID = вільно (можна призначити новий заказ).
     */
    private static final Set<OrderStatus> ACTIVE_STATUSES = EnumSet.of(
            OrderStatus.PENDING, OrderStatus.PREPARING,
            OrderStatus.READY,   OrderStatus.SERVED);

    private final OrderRepository          orderRepository;
    private final MenuItemRepository       menuItemRepository;
    private final RestaurantTableRepository tableRepository;

    // ---- Sales report -------------------------------------------------------

    @Override
    public List<SalesReportRowDto> getSalesReport(LocalDate from, LocalDate to) {
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo   = to.atTime(LocalTime.MAX);

        List<Object[]> raw = orderRepository.findSalesReport(dtFrom, dtTo);
        return raw.stream().map(row -> new SalesReportRowDto(
                (String)     row[0],
                (LocalDate)  row[1],
                ((Number)    row[2]).longValue(),
                ((Number)    row[3]).longValue(),
                (BigDecimal) row[4]
        )).toList();
    }

    // ---- Top dishes ---------------------------------------------------------

    @Override
    public List<PopularDishDto> getTopDishes(int limit) {
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

    // ---- Table occupancy ----------------------------------------------------

    @Override
    public List<TableStatusDto> getTableOccupancy(Integer restaurantId) {
        List<RestaurantTable> tables =
                tableRepository.findByRestaurantIdOrderByTableNumber(restaurantId);
        Set<Integer> orderOccupiedIds =
                Set.copyOf(orderRepository.findOccupiedTableIds(restaurantId, ACTIVE_STATUSES));
        return tables.stream()
                // Зайнятий = є активне замовлення АБО ручно визначено зайнятим
                .map(t -> new TableStatusDto(t,
                        orderOccupiedIds.contains(t.getId()) || t.isOccupied(), ""))
                .toList();
    }

    @Override
    public List<TableStatusDto> getTableOccupancyInPeriod(Integer restaurantId,
                                                          LocalDateTime from,
                                                          LocalDateTime to) {
        List<RestaurantTable> tables =
                tableRepository.findByRestaurantIdOrderByTableNumber(restaurantId);
        Set<Integer> occupiedIds = Set.copyOf(
                orderRepository.findOccupiedTableIdsInPeriod(restaurantId, from, to));
        return tables.stream()
                .map(t -> new TableStatusDto(t, occupiedIds.contains(t.getId()), ""))
                .toList();
    }
}
