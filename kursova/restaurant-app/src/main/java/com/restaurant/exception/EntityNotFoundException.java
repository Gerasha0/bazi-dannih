package com.restaurant.exception;

public class EntityNotFoundException extends RestaurantAppException {
    public EntityNotFoundException(String entityName, Object id) {
        super(entityName + " з id=" + id + " не знайдено");
    }
}
