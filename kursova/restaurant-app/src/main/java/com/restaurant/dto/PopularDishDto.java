package com.restaurant.dto;

import lombok.*;
import java.math.BigDecimal;

/** Результат аналітичного запиту — топ-страви */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopularDishDto {
    private Integer menuItemId;
    private String  name;
    private String  category;
    private BigDecimal price;
    private Long    timesOrdered;
    private BigDecimal totalRevenue;
}
