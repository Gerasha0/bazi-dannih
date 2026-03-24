package com.restaurant.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "tables",
       uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "table_number"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Positive
    @Column(name = "table_number", nullable = false)
    private int tableNumber;

    @Positive
    @Column(nullable = false)
    private int capacity;

    /** Фізична зайнятість столика (навіть після оплати). Знімається явною кнопкою. */
    @Column(name = "is_occupied", nullable = false)
    private boolean occupied = false;

    @Override
    public String toString() {
        return "Стіл №" + tableNumber + " (" + capacity + " місць)";
    }
}
