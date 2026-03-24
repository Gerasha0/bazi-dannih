package com.restaurant.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ingredients")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String unit;

    @PositiveOrZero
    @Column(name = "cost_per_unit", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal costPerUnit = BigDecimal.ZERO;

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MenuItemIngredient> menuItems = new ArrayList<>();

    @Override
    public String toString() {
        return name + " (" + unit + ")";
    }
}
