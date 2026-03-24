package com.restaurant.entity;

public enum ReservationStatus {
    CONFIRMED("Підтверджено"),
    CANCELLED("Скасовано"),
    COMPLETED("Завершено"),
    NO_SHOW("Не з'явились");

    private final String displayName;

    ReservationStatus(String displayName) {
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
