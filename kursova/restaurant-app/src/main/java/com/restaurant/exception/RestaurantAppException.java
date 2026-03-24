package com.restaurant.exception;

/** Базовий клас для бізнес-винятків програми */
public class RestaurantAppException extends RuntimeException {
    public RestaurantAppException(String message) {
        super(message);
    }
    public RestaurantAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
