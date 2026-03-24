package com.restaurant.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Рядок звіту про продажі */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportRowDto {
    private String    restaurant;
    private LocalDate saleDate;
    private Long      ordersCount;
    private Long      itemsSold;
    private BigDecimal totalRevenue;
}
