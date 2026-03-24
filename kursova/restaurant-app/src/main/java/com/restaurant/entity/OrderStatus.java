package com.restaurant.entity;

public enum OrderStatus {
    PENDING("Нове"),
    PREPARING("Готується"),
    READY("Готове"),
    SERVED("Подано"),
    PAID("Оплачено"),
    CANCELLED("Скасовано");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
