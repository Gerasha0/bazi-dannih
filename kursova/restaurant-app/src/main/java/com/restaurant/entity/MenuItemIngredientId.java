package com.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MenuItemIngredientId implements Serializable {

    @Column(name = "menu_item_id")
    private Integer menuItemId;

    @Column(name = "ingredient_id")
    private Integer ingredientId;
}
